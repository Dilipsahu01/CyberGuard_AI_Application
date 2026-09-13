cmake_minimum_required(VERSION 3.22.1)
project("cyberguard_secrets")

# Import the ONNX Runtime prebuilt shared library
add_library(onnxruntime SHARED IMPORTED)
set_target_properties(onnxruntime PROPERTIES 
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/onnx_libs/${ANDROID_ABI}/libonnxruntime.so
)

# 1. Define the library target first!
add_library(${CMAKE_PROJECT_NAME} SHARED native-lib.cpp secrets.cpp)

# 2. Include headers for ONNX Runtime and OpenSSL
target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/onnx_headers
    ${CMAKE_CURRENT_SOURCE_DIR}/openssl/include
)

# Import OpenSSL crypto library
add_library(crypto SHARED IMPORTED)
set_target_properties(crypto PROPERTIES 
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/openssl/libs/${ANDROID_ABI}/libcrypto.so
)

# 3. Link against Android logs, OpenSSL Crypto (for AES-GCM & OPENSSL_cleanse), and ONNX Runtime
target_link_libraries(${CMAKE_PROJECT_NAME}
    android
    log
    onnxruntime
    crypto
)
