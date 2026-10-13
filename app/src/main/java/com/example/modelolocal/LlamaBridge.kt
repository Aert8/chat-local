package com.example.modelolocal

import android.util.Log

class LlamaBridge {

    companion object {
        private const val TAG = "LlamaBridge"

        init {
            try {
                System.loadLibrary("native-lib")
                Log.i(TAG, "Librería nativa cargada con éxito")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Error al cargar la librería nativa", e)
            }
        }
    }

    // Callback que será invocado desde C++ por cada token generado
    var onTokenCallback: ((String) -> Unit)? = null

    // Llamado desde JNI (C++) por cada token
    @Suppress("unused")
    fun onTokenGenerated(token: String) {
        onTokenCallback?.invoke(token)
    }

    // Funciones nativas que implementaremos en C++
    external fun loadModel(
        modelPath: String,
        contextSize: Int = 2048,
        threads: Int = 4,
        batchThreads: Int = threads
    ): Boolean
    external fun generateCompletion(prompt: String, maxTokens: Int = 512): String
    external fun restoreContext(transcript: String): Boolean
    external fun resetContext()
    external fun unloadModel()
}