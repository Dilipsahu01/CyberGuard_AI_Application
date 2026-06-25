package main

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"

	"github.com/gorilla/mux"
	"github.com/rs/cors"
)

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

func main() {
	if err := os.MkdirAll(filepath.Dir(telemetryLogPath), 0755); err != nil {
		log.Fatalf("failed to create data dir: %v", err)
	}
	initDB()
	defer closeDB()

	r := mux.NewRouter()
	r.HandleFunc("/api/ping", pingHandler).Methods("GET")
	r.HandleFunc("/api/telemetry", telemetryHandler).Methods("POST")
	r.HandleFunc("/api/whitelist", whitelistHandler).Methods("GET", "POST", "DELETE")
	r.HandleFunc("/api/lora", loraHandler).Methods("POST")

	handler := cors.AllowAll().Handler(r)
	log.Println("CyberGuard-AI server starting on :8080")
	if err := http.ListenAndServe(":8080", handler); err != nil {
		log.Fatalf("server error: %v", err)
	}
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("pong"))
}

func telemetryHandler(w http.ResponseWriter, r *http.Request) {
	var payload Telemetry
	if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
		http.Error(w, "invalid JSON", http.StatusBadRequest)
		return
	}
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
