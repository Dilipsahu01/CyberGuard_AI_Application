# CyberGuard AI: Model Input/Output & Retraining Architecture

This document outlines the exact input/output pipelines, tensor shapes, and tokenization strategies for the Machine Learning models running natively on-device. This is the **Master Reference Guide** if you intend to swap, fine-tune, or retrain the models in the future.

---

## 1. Voice Activity Detection (VAD) - Silero VAD (ONNX)
**Role:** Battery saver and audio slicer. Determines if a human is speaking to prevent the heavy ASR model from wasting CPU cycles on background noise or silence.

* **Input Node:** 
  * Format: Raw PCM 16-bit Mono audio array.
  * Tensor Shape: `[1, N]` (Batch size 1, N audio samples).
  * Sample Rate: Strictly `16,000 Hz`.
  * Chunk Size: `512` samples (32 ms) or `1536` samples (96 ms).
* **Output Node:**
  * Format: Float32 Probability.
  * Tensor Shape: `[1, 1]` or scalar.
  * Value Range: `0.0` (Absolute Silence/Noise) to `1.0` (Definite Human Speech).
* **Retraining Notes:** 
  * The current model is the robust `silero_vad.onnx`. If you wish to retrain, you need an annotated dataset of speech vs. environmental noise. You must export the final model to ONNX format opset 15+.

---

## 2. Speech-to-Text (ASR) - Sherpa-ONNX Fast Conformer CTC
**Role:** Transcribes the human speech chunks into raw Hindi/English text in real-time.

* **Input Node:**
  * Format: Audio stream features (Mel-Spectrograms).
  * Feature Extraction: Handled by `sherpa-onnx` `OfflineStream` / `OnlineStream`. Audio must be `16,000 Hz` PCM 16-bit.
  * Variables Required: `tokens.txt` (The vocabulary mapping file).
* **Output Node:**
  * Format: Raw String (UTF-8).
  * Example: `"hello sir mera naam rajesh hai"`
* **Tokenization/Vocabulary:** 
  * Uses a CTC (Connectionist Temporal Classification) topology. 
  * The `tokens.txt` maps the neural network's integer output (e.g., node 54) to a character or subword (e.g., 'a' or 'ka').
* **Retraining Notes:**
  * Model architecture: Conformer (Convolution-augmented Transformer).
  * Framework: K2 / Icefall.
  * If you fine-tune this for better Indian accent recognition, you must re-export the `encoder.onnx`, `decoder.onnx`, and `joiner.onnx` (if Transducer) or a single `model.onnx` (if CTC). **INT8 Quantization** is strictly required to keep the model size below 50MB.

---

## 3. Intent NLP Vector Classifier - MiniLM-L6 (ONNX)
**Role:** Analyzes the semantics of the transcribed text to determine if the caller is using financial coercion, urgency, or authority manipulation.

* **Input Node (Tokenizer):**
  * Format: Raw String.
  * Tokenizer: WordPiece Tokenizer (requires `vocab.txt`).
  * Process: Converts string to `input_ids` and `attention_mask`. Max sequence length is clamped to `128` tokens to prevent memory overflow.
* **Input Node (Model):**
  * Tensors: `input_ids` `[1, 128]` (Int64), `attention_mask` `[1, 128]` (Int64).
* **Output Node:**
  * Tensor: `[1, 128, 384]` -> Pooled to `[1, 384]` (Float32).
  * Format: A 384-dimensional dense vector embedding.
* **Decision Logic:**
  * The resulting vector is mathematically compared (via **Cosine Similarity**) against pre-computed vectors of known scam intents (e.g., "bank account block", "police station FIR").
  * Output Score: `0.0` (Safe) to `1.0` (Scam Match).
* **Retraining Notes:**
  * The base model is `all-MiniLM-L6-v2`. 
  * To retrain, you should use SentenceTransformers (HuggingFace) and fine-tune it on a dataset of scam transcripts using `ContrastiveLoss` or `CosineSimilarityLoss`.

---

## 4. Deterministic Engines (RegexGate & Bloom Filter)
While not ML models, these are critical high-speed filters running before the NLP model.

### Keyword Gate (RegexGate)
* **Input Node:** Live ASR Transcript (String).
* **Operation:** Concurrent regular expression matching (compiled patterns).
* **Output Node:** Boolean (`true` if a hardcoded blacklisted pattern is found).
* **Updating:** Just modify the `ScamDatabase.kt` hardcoded strings. No training required.

### Number Screener (Bloom Filter)
* **Input Node:** Caller Phone Number E.164 (String).
* **Operation:** Normalization -> SHA-256 Hashing -> MurmurHash3 Bit Array mapping.
* **Output Node:** Boolean (`true` if the number is probabilistically identified as a known scammer).
* **Updating:** Requires rebuilding the 10-million bit `BitSet` payload if new scam numbers are added to the database.

---
**Summary of the Data Flow (Left to Right Nodes):**
`Raw Audio (Mic)` -> `[VAD Model]` -> `[ASR Model]` -> `Raw Text` -> `[RegexGate]` -> `[NLP Tokenizer]` -> `[MiniLM Model]` -> `384D Vector` -> `[Cosine Similarity]` -> `Risk Score %`
