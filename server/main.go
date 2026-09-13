package main

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/mux"
	"github.com/rs/cors"
)

var startTime = time.Now()

// Telemetry represents a scam-detection event payload from the Android client.
type Telemetry struct {
	Caller       string    `json:"caller"`
	Score        int       `json:"score"`
	Transcript   string    `json:"transcript"`
	IntentScores []float64 `json:"intent_scores"`
	Timestamp    int64     `json:"timestamp"`
}

// WhitelistEntry represents a trusted number entry with an expiry timestamp.
type WhitelistEntry struct {
	Number string `json:"number"`
	Expiry int64  `json:"expiry"`
}

var (
	// In-memory whitelist (number → expiry epoch ms)
	whitelist = make(map[string]int64)
	// File path for persisted telemetry logs
	telemetryLogPath = "data/telemetry.log"
)

// Simple In-Memory Rate Limiter for DDoS Protection
type ClientRate struct {
	Count    int
	LastSeen time.Time
}

var (
	rateLimits = make(map[string]*ClientRate)
	rateMutex  sync.Mutex
)

// rateLimitMiddleware blocks IPs or Device-IDs exceeding 20 requests per minute
func rateLimitMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		clientIP := strings.Split(r.RemoteAddr, ":")[0]
		deviceID := r.Header.Get("X-Device-ID")
		
		rateKey := clientIP
		if deviceID != "" {
			rateKey = deviceID
		}

		rateMutex.Lock()
		client, exists := rateLimits[rateKey]
		if !exists {
			rateLimits[rateKey] = &ClientRate{Count: 1, LastSeen: time.Now()}
			rateMutex.Unlock()
			next.ServeHTTP(w, r)
			return
		}

		if time.Since(client.LastSeen) > time.Minute {
			client.Count = 0
			client.LastSeen = time.Now()
		}

		client.Count++
		if client.Count > 20 {
			rateMutex.Unlock()
			log.Printf("Rate limit exceeded for %s", rateKey)
			http.Error(w, "Too Many Requests", http.StatusTooManyRequests)
			return
		}
		rateMutex.Unlock()

		next.ServeHTTP(w, r)
	})
}

func main() {
	if err := os.MkdirAll(filepath.Dir(telemetryLogPath), 0755); err != nil {
		log.Fatalf("failed to create data dir: %v", err)
	}
	initDB()
	defer closeDB()

	r := mux.NewRouter()
	r.HandleFunc("/api/ping", pingHandler).Methods("GET")
	r.HandleFunc("/api/health", healthHandler).Methods("GET")
	r.HandleFunc("/api/telemetry", telemetryHandler).Methods("POST")
	r.HandleFunc("/api/whitelist", whitelistHandler).Methods("GET", "POST", "DELETE")
	r.HandleFunc("/api/lora", loraHandler).Methods("POST")

	handler := cors.AllowAll().Handler(rateLimitMiddleware(r))
	srv := &http.Server{
		Addr:         ":8080",
		Handler:      handler,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  60 * time.Second,
	}
	log.Println("CyberGuard-AI server starting on :8080")
	if err := srv.ListenAndServe(); err != nil {
		log.Fatalf("server error: %v", err)
	}
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("pong"))
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"status":  "ok",
		"uptime":  time.Since(startTime).String(),
		"service": "cyberguard-ai-swarm-server",
	})
}

var telemetryPepper = []byte("CyberGuard_DPDP_Telemetry_Key_32")

func decryptCaller(encoded string) (string, error) {
	data, err := base64.URLEncoding.DecodeString(encoded)
	if err != nil {
		return "", err
	}
	if len(data) < 12 {
		return "", fmt.Errorf("ciphertext too short")
	}
	iv := data[:12]
	ciphertext := data[12:]

	block, err := aes.NewCipher(telemetryPepper)
	if err != nil {
		return "", err
	}

	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}

	plaintext, err := aesgcm.Open(nil, iv, ciphertext, nil)
	if err != nil {
		return "", err
	}
	return string(plaintext), nil
}

func parseBinaryTelemetry(data []byte) (Telemetry, error) {
	if len(data) < 11 {
		return Telemetry{}, fmt.Errorf("payload too short")
	}
	
	bitOffset := 0
	readBits := func(numBits int) int {
		v := 0
		bitsRead := 0
		for bitsRead < numBits {
			byteIndex := bitOffset / 8
			bitIndex := bitOffset % 8
			bitsAvailableInByte := 8 - bitIndex
			bitsToGet := numBits - bitsRead
			if bitsToGet > bitsAvailableInByte {
				bitsToGet = bitsAvailableInByte
			}
			
			mask := (1 << bitsToGet) - 1
			extracted := (int(data[byteIndex]) >> bitIndex) & mask
			v |= (extracted << bitsRead)
			
			bitOffset += bitsToGet
			bitsRead += bitsToGet
		}
		return v
	}
	
	var t Telemetry
	_ = readBits(14) // caller hash prefix
	_ = readBits(4)  // bloom, voip, stir, rapid
	_ = readBits(16) // daysKnown, totalCalls
	_ = readBits(6)  // regexScore
	
	intimacy := float64(readBits(7))
	urgency := float64(readBits(7))
	trust := float64(readBits(7))
	financial := float64(readBits(7))
	coercion := float64(readBits(7))
	t.IntentScores = []float64{intimacy, urgency, trust, financial, coercion}
	
	t.Score = readBits(7)
	t.Timestamp = time.Now().UnixMilli()
	
	return t, nil
}

func telemetryHandler(w http.ResponseWriter, r *http.Request) {
	encryptedCaller := r.Header.Get("X-Swarm-Caller")
	var caller string
	if encryptedCaller != "" {
		decrypted, err := decryptCaller(encryptedCaller)
		if err != nil {
			log.Printf("Failed to decrypt caller ID: %v", err)
			caller = "Unknown (Decryption Failed)"
		} else {
			caller = decrypted
		}
	} else {
		caller = "Unknown (No Header)"
	}

	body, err := io.ReadAll(r.Body)
	if err != nil || len(body) == 0 {
		http.Error(w, "invalid or empty payload", http.StatusBadRequest)
		return
	}

	appSignature := r.Header.Get("X-App-Signature")
	if appSignature == "" {
		http.Error(w, "missing app signature", http.StatusUnauthorized)
		return
	}

	// Recalculate HMAC-SHA256 to prove authenticity
	mac := hmac.New(sha256.New, telemetryPepper)
	mac.Write(body)
	expectedMAC := mac.Sum(nil)
	expectedBase64 := base64.URLEncoding.EncodeToString(expectedMAC)
	// Some encoders use NO_WRAP without padding, handle trailing = differences
	if strings.TrimRight(appSignature, "=") != strings.TrimRight(expectedBase64, "=") {
		log.Printf("App spoofing detected! Invalid HMAC signature from %s", r.RemoteAddr)
		http.Error(w, "invalid app signature", http.StatusForbidden)
		return
	}

	payload, err := parseBinaryTelemetry(body)
	if err != nil {
		log.Printf("failed to parse binary telemetry: %v", err)
		http.Error(w, "invalid binary payload", http.StatusBadRequest)
		return
	}
	payload.Caller = caller

	if err := saveTelemetry(payload); err != nil {
		log.Printf("failed to save telemetry: %v", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	log.Printf("telemetry saved: caller=%s score=%d", payload.Caller, payload.Score)
	w.WriteHeader(http.StatusAccepted)
	w.Write([]byte("received"))
}

func whitelistHandler(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case "GET":
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(whitelist)
	case "POST":
		var entry WhitelistEntry
		if err := json.NewDecoder(r.Body).Decode(&entry); err != nil {
			http.Error(w, "invalid JSON", http.StatusBadRequest)
			return
		}
		whitelist[entry.Number] = entry.Expiry
		w.WriteHeader(http.StatusCreated)
		w.Write([]byte("added"))
	case "DELETE":
		number := r.URL.Query().Get("number")
		if number != "" {
			delete(whitelist, number)
		}
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("removed"))
	}
}

// loraHandler handles the 9-byte LoRa/SMS binary fallback payload.
// Format: 4B caller hash + 1B score + 4B timestamp.
func loraHandler(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(io.LimitReader(r.Body, 64*1024))
	if err != nil {
		http.Error(w, "cannot read body", http.StatusBadRequest)
		return
	}
	if len(body) != 9 {
		http.Error(w, "payload must be exactly 9 bytes", http.StatusBadRequest)
		return
	}
	log.Printf("LoRa payload received: %x", body)
	w.WriteHeader(http.StatusAccepted)
	w.Write([]byte("lora received"))
}
