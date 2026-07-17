# CyberGuard-AI: Master Training & Deployment Specification

This document serves as the absolute single source of truth for the continuous fine-tuning, quantization, evaluation, and deployment of the CyberGuard-AI inference stack. It provides all necessary parameters, schemas, and hyperparameters required to re-create the production model weights from scratch.

---

## 1. Data Engineering & Preparation

To guarantee zero regression during on-device execution, all training datasets **must** exactly mirror the runtime pipeline’s pre-processing behavior.

### 1.1 IntentNLP Training Schema (JSONL)
All fine-tuning datasets for the MiniLM-L6 classifier must adhere to the following strict JSON schema:
```json
{
  "transcript": "string (Max 100 words, reflecting the runtime sliding window)",
  "labels": {
    "financial": "float (0.0 - 1.0)",
    "coercion": "float (0.0 - 1.0)",
    "urgency": "float (0.0 - 1.0)",
    "intimacy": "float (0.0 - 1.0)",
    "trust": "float (0.0 - 1.0)"
  }
}
```

### 1.2 ASR Audio Preprocessing constraints
*   **Sample Rate**: `16,000 Hz` strictly.
*   **Channels**: `Mono` (1-channel).
*   **Bit Depth**: `16-bit PCM`.
*   **Normalization**: Audio arrays must be scaled to Float32 in the range `[-1.0f, 1.0f]` by dividing `short` values by `32768.0f`.
*   **Chunk Slicing**: Train on randomized chunk lengths mimicking the 100ms VAD output buffers to ensure stream-resilience.

---

## 2. Architecture & Tensor Definitions

### 2.1 IntentNLP (MiniLM-L6)
*   **Base Model**: `all-MiniLM-L6-v2`
*   **Input Tensor Shape**: `[batch_size, sequence_length]` (where max `sequence_length = 128` corresponding to the 100-word sliding window).
*   **Embedding Dimension**: `384`
*   **Classification Head Projection (5-Class Mapping)**:
    *   `Class 0`: Financial Intent
    *   `Class 1`: Coercion / Extortion
    *   `Class 2`: Urgency / Time-Pressure
    *   `Class 3`: Intimacy (Romance Scam baseline)
    *   `Class 4`: Trust / Authority Figure Impersonation

### 2.2 Speech-to-Text ASR (Fast Conformer CTC)
*   **Base Architecture**: NVIDIA NeMo Fast-Conformer CTC (Sherpa-ONNX compatible).
*   **Tokenizer**: BPE (Byte-Pair Encoding).
*   **Vocabulary File**: `vocab.txt` and `tokens.txt` (must map perfectly to the INT8 decoder definitions).
*   **Input Tensor Shape**: `[batch_size, feature_dim, time_steps]` (typically `feature_dim = 80` for Log-Mel Spectrograms).
*   **Output Tensor Shape**: `[batch_size, time_steps, vocab_size]` (CTC Logits).

---

## 3. Fine-Tuning & Training Loop

The baseline models must be fine-tuned using the following locked hyperparameters to avoid catastrophic forgetting and maintain edge-compute latency bounds.

### 3.1 Hyperparameter Configuration (IntentNLP)
```yaml
optimizer: AdamW
learning_rate: 2e-5
weight_decay: 0.01
epochs: 3 to 5 (Early stopping based on validation loss)
batch_size: 32 (Per GPU)
warmup_steps: 10% of total training steps
loss_function: BCEWithLogitsLoss (Multi-label classification)
```

### 3.2 "Gold Standard" Evaluation Metrics
Before any weights are promoted to deployment, they must pass the following rigid thresholds:
*   **Macro F1-Score**: `> 0.92` (Ensures balanced detection across all 5 classes).
*   **Recall (Financial & Coercion)**: `> 0.96` (False Negatives are unacceptable in these critical categories).
*   **Precision (Trust)**: `> 0.90` (Minimizes False Positives on legitimate banking/support calls).
*   **On-Device Latency Bound**: `< 17ms` average execution time per inference pass on Android hardware (e.g., Snapdragon 8 Gen 1).

---

## 4. Quantization & Conversion (The Bridge)

Models trained in FP32/FP16 must be converted to INT8 ONNX Runtime format (`.ort`) to fit within the `145MB` total application footprint constraints.

### 4.1 PyTorch to ONNX Export
Export the fine-tuned PyTorch model with dynamic axes to support variable-length inputs:
```python
torch.onnx.export(
    model,
    dummy_input,
    "model_fp32.onnx",
    opset_version=14,
    input_names=['input_ids', 'attention_mask'],
    output_names=['logits'],
    dynamic_axes={
        'input_ids': {0: 'batch_size', 1: 'sequence_length'},
        'attention_mask': {0: 'batch_size', 1: 'sequence_length'},
        'logits': {0: 'batch_size'}
    }
)
```

### 4.2 INT8 Quantization Strategy
Use Dynamic Quantization to compress the weights to INT8. This provides the best balance of footprint reduction (~4x) with minimal accuracy degradation (< 1.5% F1 drop).
```python
from onnxruntime.quantization import quantize_dynamic, QuantType

quantize_dynamic(
    model_input="model_fp32.onnx",
    model_output="model_int8.onnx",
    weight_type=QuantType.QInt8
)
```

### 4.3 ORT Format Conversion
Convert the ONNX file to the highly optimized `.ort` format for Edge execution:
```bash
python -m onnxruntime.tools.convert_onnx_models_to_ort model_int8.onnx
```

---

## 5. Regression & Validation Protocol

### 5.1 Regex Fallback Criteria
The Neural Network is probabilistic; the `RegexGate` is deterministic. The Ensemble Engine must respect the following fallback protocol:
*   **Confidence Threshold**: If the normalized logit confidence for any threat class is `< 0.65` (Ambiguous), the `RegexGate` output is heavily weighted.
*   **Hardware Interrupt**: If the `RegexGate` score is `>= 35` (e.g., `"digital arrest"`, `"OTP"`), it **must** instantly override a benign NLP classification and trigger the threat banner.

### 5.2 Critical Threat Vector Validation
The evaluation dataset **must** contain a held-out verification set covering the 19 critical Indian threat vectors. The fine-tuned model must not regress on:
1.  Law Enforcement Impersonation (`CBI`, `Digital Arrest`).
2.  Utility Disconnect Scams (`Electricity cut`).
3.  Remote Access Trojans (`AnyDesk`, `TeamViewer`).
4.  Carrier Impersonation (`TRAI`, `SIM Block`).

Any new model candidate that causes a False Negative on these specific scenarios during CI/CD evaluation must be immediately rejected.
