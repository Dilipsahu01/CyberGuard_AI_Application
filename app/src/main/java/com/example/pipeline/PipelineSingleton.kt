package com.example.pipeline

import android.content.Context
import android.util.Log

object PipelineSingleton {
    private var instance: PipelineManager? = null

    @Synchronized
    fun getInstance(context: Context): PipelineManager {
        if (instance == null) {
            Log.i("PipelineSingleton", "Initializing PipelineManager (Singleton)...")
            instance = PipelineManager(context.applicationContext)
        }
        return instance!!
    }

    @Synchronized
    fun clear() {
        instance?.close()
        instance = null
        Log.i("PipelineSingleton", "PipelineManager resources released and singleton cleared.")
    }
}
