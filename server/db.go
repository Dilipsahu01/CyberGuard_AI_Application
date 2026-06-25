package main

import (
	"database/sql"
	"encoding/json"
	"log"

	_ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

// dbPath is the SQLite database file path.
// Tests override this to ":memory:" to avoid filesystem dependencies.
var dbPath = "data/cyberguard.db"

func initDB() {
	var err error
	db, err = sql.Open("sqlite3", dbPath)
	if err != nil {
		log.Fatalf("failed to open sqlite db: %v", err)
	}
	createStmt := `CREATE TABLE IF NOT EXISTS telemetry (
		id             INTEGER PRIMARY KEY AUTOINCREMENT,
		caller         TEXT,
		score          INTEGER,
		transcript     TEXT,
		intent_scores  TEXT,
		timestamp      INTEGER
	);`
	if _, err := db.Exec(createStmt); err != nil {
		log.Fatalf("failed to create telemetry table: %v", err)
	}
}

func saveTelemetry(t Telemetry) error {
	intentJSON, err := json.Marshal(t.IntentScores)
	if err != nil {
		return err
	}
	_, err = db.Exec(
		`INSERT INTO telemetry (caller, score, transcript, intent_scores, timestamp) VALUES (?,?,?,?,?)`,
		t.Caller, t.Score, t.Transcript, string(intentJSON), t.Timestamp,
	)
	return err
}

func closeDB() {
	if db != nil {
		_ = db.Close()
	}
}
