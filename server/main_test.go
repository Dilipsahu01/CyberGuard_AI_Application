package main

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

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
	initDB()
	defer closeDB()

	body := `{"caller":"abc123","score":42,"transcript":"test","intent_scores":[1,2,3,4,5],"timestamp":1234567890}`
	req := httptest.NewRequest(http.MethodPost, "/api/telemetry", strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	telemetryHandler(w, req)
	resp := w.Result()
	if resp.StatusCode != http.StatusAccepted {
		t.Fatalf("expected 202 Accepted, got %d", resp.StatusCode)
	}
}

func TestTelemetryHandler_InvalidJSON(t *testing.T) {
	req := httptest.NewRequest(http.MethodPost, "/api/telemetry", strings.NewReader("not-json"))
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
