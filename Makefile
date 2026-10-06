.PHONY: server test-ai encrypt-models clean

# Default help command
help:
	@echo "CyberGuard AI Commands:"
	@echo "  make server        - Start the Go Swarm Server"
	@echo "  make test-ai       - Run the AI model tests"
	@echo "  make encrypt-models- Encrypt the ONNX models"
	@echo "  make clean         - Clean up scratch logs and files"

server:
	@echo "Starting CyberGuard Swarm Server..."
	cd server && go run main.go

test-ai:
	@echo "Running AI Model tests..."
	python3 scripts/test_model.py
	python3 scripts/test_vad.py

encrypt-models:
	@echo "Encrypting ONNX Models for Android deployment..."
	python3 scripts/encrypt_models.py

clean:
	@echo "Cleaning up log files..."
	rm -f *.txt
	rm -rf __pycache__
	rm -rf scripts/__pycache__
	@echo "Clean complete."
