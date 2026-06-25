package com.k2fsa.sherpa.onnx

fun getFeatureConfig(sampleRate: Int = 16000, featureDim: Int = 80): FeatureConfig {
    return FeatureConfig(
        sampleRate = sampleRate,
        featureDim = featureDim,
    )
}

fun getOnlineRecognizerConfig(
    modelDir: String,
    type: String,
    numThreads: Int = 2
): OnlineRecognizerConfig {
    val featConfig = getFeatureConfig()
    
    val modelConfig = OnlineModelConfig()
    modelConfig.numThreads = numThreads
    modelConfig.tokens = "$modelDir/tokens.txt"
    modelConfig.debug = false
    
    when (type) {
        "nemo_ctc" -> {
            modelConfig.neMoCtc.model = "$modelDir/model.int8.onnx"
        }
        "zipformer2" -> {
            modelConfig.zipformer2Ctc.model = "$modelDir/model.int8.onnx"
        }
    }
    
    val endpointConfig = EndpointConfig()
    
    return OnlineRecognizerConfig(
        featConfig = featConfig,
        modelConfig = modelConfig,
        endpointConfig = endpointConfig,
        enableEndpoint = true,
        decodingMethod = "greedy_search"
    )
}
