package com.example

import org.junit.Test
import org.junit.Assert.*
import net/http
import net/http/httptest
import "testing"

func TestTelemetryHandler(t *testing.T) {
    // Setup test server with the real handler
    reqBody := `{"caller":"abc123","score":42,"transcript":"test","intent_scores":[1,2,3,4,5],"timestamp":1234567890}`
    req := httptest.NewRequest("POST", "/api/telemetry", strings.NewReader(reqBody))
    w := httptest.NewRecorder()
    // Assume handler is exported as TelemetryHandler
    TelemetryHandler(w, req)
    resp := w.Result()
    if resp.StatusCode != http.StatusOK {
        t.Fatalf("expected 200 OK, got %d", resp.StatusCode)
    }
    // Verify DB insertion (using in‑memory DB or mock) – omitted for brevity
}
