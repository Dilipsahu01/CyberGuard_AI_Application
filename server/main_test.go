package main

import (
	"bytes"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func signPayload(payload []byte) string {
	mac := hmac.New(sha256.New, telemetryPepper)
	mac.Write(payload)
	return base64.URLEncoding.EncodeToString(mac.Sum(nil))
}

func TestPingHandler(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/api/ping", nil)
	w := httptest.NewRecorder()
	pingHandler(w, req)
	resp := w.Result()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d", resp.StatusCode)
	}
}

func TestTelemetryHandler_ValidPayload(t *testing.T) {
	dbPath = ":memory:"
	initDB()
	defer closeDB()

	body := []byte(`{"caller":"abc123","score":42,"transcript":"test","intent_scores":[1,2,3,4,5],"timestamp":1234567890}`)
	req := httptest.NewRequest(http.MethodPost, "/api/telemetry", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-App-Signature", signPayload(body))
	w := httptest.NewRecorder()
	telemetryHandler(w, req)
	resp := w.Result()
	if resp.StatusCode != http.StatusAccepted {
		t.Fatalf("expected 202 Accepted, got %d", resp.StatusCode)
	}
}

func TestTelemetryHandler_InvalidJSON(t *testing.T) {
	body := []byte("not-json")
	req := httptest.NewRequest(http.MethodPost, "/api/telemetry", bytes.NewReader(body))
	req.Header.Set("X-App-Signature", signPayload(body))
	w := httptest.NewRecorder()
	telemetryHandler(w, req)
	resp := w.Result()
	if resp.StatusCode != http.StatusBadRequest {
		t.Fatalf("expected 400 Bad Request, got %d", resp.StatusCode)
	}
}

func TestLoraHandler_WrongSize(t *testing.T) {
	req := httptest.NewRequest(http.MethodPost, "/api/lora", strings.NewReader("short"))
	w := httptest.NewRecorder()
	loraHandler(w, req)
	resp := w.Result()
	if resp.StatusCode != http.StatusBadRequest {
		t.Fatalf("expected 400 Bad Request, got %d", resp.StatusCode)
	}
}

func TestHealthHandler(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/api/health", nil)
	w := httptest.NewRecorder()
	healthHandler(w, req)
	resp := w.Result()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d", resp.StatusCode)
	}
	if ct := resp.Header.Get("Content-Type"); ct != "application/json" {
		t.Fatalf("expected application/json, got %s", ct)
	}
}
