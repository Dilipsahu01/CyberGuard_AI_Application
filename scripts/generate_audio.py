from gtts import gTTS
import os
import subprocess

tts = gTTS("Hello! This is a call from your bank. We detected suspicious activity. Please provide your O T P immediately.", lang='en')
tts.save("scam_temp.mp3")

# Convert to 16kHz, 16-bit, Mono WAV
subprocess.run([
    "ffmpeg", "-y", "-i", "scam_temp.mp3",
    "-ac", "1", "-ar", "16000", "-sample_fmt", "s16",
    "/home/dilip_sahu/5ghack/cyberguard-ai-app/app/src/main/assets/scam_test.wav"
])
os.remove("scam_temp.mp3")
print("Audio generated successfully!")
