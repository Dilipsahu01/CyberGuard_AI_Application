import onnxruntime as ort

env = ort.InferenceSession("/home/dilip_sahu/5ghack/cyberguard-ai-app/app/src/main/assets/models/silero_vad.ort.enc")
for output in env.get_outputs():
    print(output.name, output.shape)
