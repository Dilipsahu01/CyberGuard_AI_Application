package main

import (
    "encoding/json"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "time"

    "github.com/gorilla/mux"
    "github.com/rs/cors"
)


import (
    "encoding/json"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "time"

    "github.com/gorilla/mux"
    "github.com/rs/cors"
)

type Telemetry struct {
    Caller        string    `json:"caller"`
    Score         int       `json:"score"`
    Transcript    string    `json:"transcript"`
    IntentScores  []float64 `json:"intent_scores"`
    Timestamp     int64     `json:"timestamp"`
}

type WhitelistEntry struct {
    Number string `json:"number"`
    Expiry int64  `json:"expiry"`
}

var (
    // In‑memory whitelist (number → expiry epoch ms)
    whitelist = make(map[string]int64)
    // File path for persisted telemetry logs (JSON lines)
    telemetryLogPath = "data/telemetry.log"
)

func main() {
    // Ensure data directory exists
    if err := os.MkdirAll(filepath.Dir(telemetryLogPath), 0755); err != nil {
        log.Fatalf("failed to create data dir: %v", err)
    }
    // Initialize SQLite DB
    initDB()
    defer closeDB()

    r := mux.NewRouter()
    r.HandleFunc("/api/ping", pingHandler).Methods("GET")
    r.HandleFunc("/api/telemetry", telemetryHandler).Methods("POST")
    r.HandleFunc("/api/whitelist", whitelistHandler).Methods("GET", "POST", "DELETE")
    r.HandleFunc("/api/lora", loraHandler).Methods("POST")

    // CORS – allow requests from any origin (replace with tighter policy if needed)
    handler := cors.AllowAll().Handler(r)
    log.Println("⚡️ CyberGuard‑AI server starting on :8080 …")
    if err := http.ListenAndServe(":8080", handler); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

// ---------- Handlers ----------
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
    // Persist telemetry to SQLite DB
    if err := saveTelemetry(payload); err != nil {
        log.Printf("failed to save telemetry: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    log.Printf("📡 telemetry saved: %s score=%d intents=%v", payload.Caller, payload.Score, payload.IntentScores)
    w.WriteHeader(http.StatusAccepted)
    w.Write([]byte("received"))
}

func whitelistHandler(w http.ResponseWriter, r *http.Request) {
    switch r.Method {
    case "GET":
        // Return current whitelist as JSON map number → expiry
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

// ---------- LoRa / binary‑SMS fallback ----------
// Expected payload: 9 bytes (caller hash 4 bytes, score 1 byte, timestamp 4 bytes)
func loraHandler(w http.ResponseWriter, r *http.Request) {
    // Read raw body (max 64 KB just in case)
    body, err := io.ReadAll(io.LimitReader(r.Body, 64*1024))
    if err != nil {
        http.Error(w, "cannot read body", http.StatusBadRequest)
        return
    }
    if len(body) != 9 {
        http.Error(w, "payload must be exactly 9 bytes", http.StatusBadRequest)
        return
    }
    // For demo purposes just log the raw bytes as hex
    log.Printf("📶 LoRa payload received: %x", body)
    // In a real system you would decode the caller hash, score, timestamp and act on it.
    w.WriteHeader(http.StatusAccepted)
    w.Write([]byte("lora received"))
}
