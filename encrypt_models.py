import os
import hashlib
from Crypto.Cipher import AES
import secrets

# V1.1_Updates Section 1: Advanced Cryptography (Model Encryption Script)
# Run this script on your laptop BEFORE building the APK.

ASSETS_DIR = "app/src/main/assets"
# We will use this exact key in the Android C++ NativeSecrets layer to decrypt.
# In a real enterprise app, this key would be fetched over HTTPS after user auth.
MASTER_KEY = b"CyberGuard_Secure_Model_Key_2026" 

def encrypt_file(file_path):
    if not file_path.endswith((".onnx", ".ort", ".pt")):
        return

    print(f"Encrypting: {file_path}")
    
    with open(file_path, "rb") as f:
        plaintext = f.read()
        
    # Calculate Original SHA-256 (for integrity checking in Android)
    sha256_hash = hashlib.sha256(plaintext).hexdigest()
    print(f"  -> Original SHA-256: {sha256_hash}")

    # Encrypt using AES-GCM
    # Generate a random 12-byte initialization vector (IV) for GCM
    iv = os.urandom(12)
    cipher = AES.new(MASTER_KEY, AES.MODE_GCM, nonce=iv)
    ciphertext, tag = cipher.encrypt_and_digest(plaintext)
    
    # Save the encrypted file (IV + Tag + Ciphertext)
    encrypted_path = file_path + ".enc"
    with open(encrypted_path, "wb") as f:
        f.write(iv)
        f.write(tag)
        f.write(ciphertext)
        
    print(f"  -> Saved as {encrypted_path}")
    # os.remove(file_path) # In production, we'd delete the raw file so it's not in the APK

def main():
    print("Starting Model Encryption...")
    for root, _, files in os.walk(ASSETS_DIR):
        for file in files:
            encrypt_file(os.path.join(root, file))
    
    print("\nEncryption Complete. Copy the SHA-256 hashes above to your ModelIntegrityVerifier.kt.")

if __name__ == "__main__":
    main()
