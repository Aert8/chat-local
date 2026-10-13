#include <jni.h>
#include <string>
#include <vector>
#include <dlfcn.h>
#include <cstring>
#include <android/log.h>
#include "llama.h"
#include "ggml-backend.h"

#define TAG "LFM_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model*   g_model  = nullptr;
static llama_context* g_ctx    = nullptr;
static int            g_n_past = 0; // Rastrea cuántos tokens hay acumulados en la memoria KV

#define JNI_FN(name) Java_com_example_modelolocal_LlamaBridge_##name

extern "C" {

JNIEXPORT jboolean JNICALL
JNI_FN(loadModel)(JNIEnv* env, jobject /* this */, jstring model_path, jint context_size, jint threads, jint batch_threads) {
    if (g_model != nullptr) {
        LOGI("Liberando modelo previo...");
        llama_free(g_ctx);
        llama_model_free(g_model);
        g_ctx = nullptr;
        g_model = nullptr;
    }

    g_n_past = 0; // Reiniciar contador de contexto

    const char* path_chars = env->GetStringUTFChars(model_path, nullptr);
    std::string path(path_chars);
    env->ReleaseStringUTFChars(model_path, path_chars);

    llama_backend_init();

    void *vulkan_handle = dlopen("libvulkan.so", RTLD_NOW | RTLD_LOCAL);
    const char *vulkan_error = vulkan_handle ? nullptr : dlerror();
    LOGI("Loader Vulkan: %s%s", vulkan_handle ? "cargado" : "fallo: ",
         vulkan_error ? vulkan_error : "");
    void *ggml_vulkan_handle = dlopen("libggml-vulkan.so", RTLD_NOW | RTLD_LOCAL);
    const char *ggml_vulkan_error = ggml_vulkan_handle ? nullptr : dlerror();
    LOGI("Biblioteca ggml Vulkan: %s%s", ggml_vulkan_handle ? "cargada" : "fallo: ",
         ggml_vulkan_error ? ggml_vulkan_error : "");

    llama_model_params model_params = llama_model_default_params();
    LOGI("Backends ggml registrados: %zu", ggml_backend_reg_count());
    for (size_t i = 0; i < ggml_backend_reg_count(); ++i) {
        ggml_backend_reg_t reg = ggml_backend_reg_get(i);
        LOGI("Backend ggml[%zu]: %s, dispositivos=%zu",
             i, ggml_backend_reg_name(reg), ggml_backend_reg_dev_count(reg));
    }
    const bool gpu_available = llama_supports_gpu_offload();
    LOGI("llama_supports_gpu_offload=%s", gpu_available ? "true" : "false");
    bool adreno_642l = false;
    for (size_t i = 0; i < ggml_backend_dev_count(); ++i) {
        ggml_backend_dev_t device = ggml_backend_dev_get(i);
        const auto device_type = ggml_backend_dev_type(device);
        if (device_type == GGML_BACKEND_DEVICE_TYPE_GPU ||
            device_type == GGML_BACKEND_DEVICE_TYPE_IGPU) {
            const char * description = ggml_backend_dev_description(device);
            adreno_642l = adreno_642l ||
                    (description != nullptr && std::strstr(description, "Adreno (TM) 642L") != nullptr);
            LOGI("Dispositivo GPU detectado: %s (%s), tipo=%d",
                 ggml_backend_dev_name(device),
                 description,
                 static_cast<int>(device_type));
        }
    }
    const bool gpu_offload_enabled = gpu_available && !adreno_642l;
    if (adreno_642l) {
        LOGW("Offload Vulkan desactivado para Adreno 642L por SIGSEGV en reserva de tensores.");
    }
    model_params.n_gpu_layers = gpu_offload_enabled ? -1 : 0;
    ggml_backend_dev_t cpu_device = ggml_backend_dev_by_type(GGML_BACKEND_DEVICE_TYPE_CPU);
    ggml_backend_dev_t cpu_devices[] = {cpu_device, nullptr};
    model_params.devices = gpu_offload_enabled ? nullptr : cpu_devices;
    model_params.split_mode = LLAMA_SPLIT_MODE_LAYER;
    g_model = llama_model_load_from_file(path.c_str(), model_params);
    if (!g_model) {
        if (gpu_offload_enabled) {
            LOGW("No se pudo cargar con GPU; reintentando en CPU.");
            model_params = llama_model_default_params();
            model_params.n_gpu_layers = 0;
            g_model = llama_model_load_from_file(path.c_str(), model_params);
        }
        if (!g_model) {
            LOGE("No se pudo cargar el archivo GGUF: %s", path.c_str());
            return JNI_FALSE;
        }
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = context_size;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = batch_threads;
    ctx_params.n_batch = 512;
    ctx_params.n_ubatch = 128;
    ctx_params.offload_kqv = gpu_offload_enabled && model_params.n_gpu_layers != 0;
    ctx_params.op_offload = ctx_params.offload_kqv;

    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        if (gpu_offload_enabled && model_params.n_gpu_layers != 0) {
            LOGW("No se pudo crear contexto GPU; reintentando en CPU.");
            llama_model_free(g_model);
            g_model = nullptr;
            model_params = llama_model_default_params();
            model_params.n_gpu_layers = 0;
            g_model = llama_model_load_from_file(path.c_str(), model_params);
            if (g_model) {
                ctx_params.offload_kqv = false;
                ctx_params.op_offload = false;
                g_ctx = llama_init_from_model(g_model, ctx_params);
            }
        }
        if (!g_ctx) {
            LOGE("Fallo al crear el contexto");
            llama_model_free(g_model);
            g_model = nullptr;
            return JNI_FALSE;
        }
    }

    LOGI("Modelo cargado. backend_gpu=%s, generation_threads=%d, batch_threads=%d, contexto=%d",
         model_params.n_gpu_layers != 0 ? "true" : "false",
         threads, batch_threads, context_size);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
JNI_FN(generateCompletion)(JNIEnv* env, jobject thiz, jstring prompt_str, jint max_tokens) {
    if (!g_model || !g_ctx) {
        return env->NewStringUTF("[Error: El modelo no está cargado]");
    }

    const char* prompt_chars = env->GetStringUTFChars(prompt_str, nullptr);
    std::string raw_prompt(prompt_chars);
    env->ReleaseStringUTFChars(prompt_str, prompt_chars);

    const int n_ctx = llama_n_ctx(g_ctx);

    // 1. Formato de turno conversacional ChatML
    std::string formatted_turn;

    // Inyectar system prompt en el primer turno
    if (g_n_past == 0) {
        formatted_turn = "<|im_start|>system\nResponde de forma concisa y útil en español. Usa markdown cuando sea apropiado para formatear tu respuesta.<|im_end|>\n";
    }

    formatted_turn += "<|im_start|>user\n" + raw_prompt + "<|im_end|>\n<|im_start|>assistant\n";

    const llama_vocab* vocab = llama_model_get_vocab(g_model);

    // 2. Tokenizar la nueva entrada
    // Si ya hay contexto acumulado (g_n_past > 0), no agregamos token especial de inicio (BOS)
    bool add_bos = (g_n_past == 0);
    std::vector<llama_token> tokens(formatted_turn.size() + 16);
    int n_tokens = llama_tokenize(vocab, formatted_turn.c_str(), formatted_turn.length(),
                                  tokens.data(), tokens.size(), add_bos, true);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, formatted_turn.c_str(), formatted_turn.length(),
                                  tokens.data(), tokens.size(), add_bos, true);
    }
    tokens.resize(n_tokens);

    // 3. Verificar si el nuevo turno cabe en la ventana de contexto
    if (g_n_past + n_tokens >= n_ctx) {
        LOGW("¡Límite de ventana de contexto alcanzado! (%d + %d >= %d)", g_n_past, n_tokens, n_ctx);
        return env->NewStringUTF("[Límite alcanzado: La memoria de contexto está llena. Inicia una nueva conversación]");
    }

    LOGI("Turno del usuario tokenizado: %d tokens. Contexto actual: %d / %d (%.1f%%)",
         n_tokens, g_n_past + n_tokens, n_ctx, ((g_n_past + n_tokens) * 100.0f) / n_ctx);

    // 4. Crear lote (batch) asignando las posiciones continuas en el KV cache
    llama_batch batch = llama_batch_init(n_tokens, 0, 1);
    for (int i = 0; i < n_tokens; i++) {
        batch.token[i]     = tokens[i];
        batch.pos[i]       = g_n_past + i;
        batch.n_seq_id[i]  = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i]    = (i == n_tokens - 1); // Solo calcular logits del último token
    }
    batch.n_tokens = n_tokens;

    if (llama_decode(g_ctx, batch) != 0) {
        llama_batch_free(batch);
        LOGE("Error en llama_decode al procesar el prompt");
        return env->NewStringUTF("[Error al decodificar prompt]");
    }
    llama_batch_free(batch);
    g_n_past += n_tokens;

    // 5. Configurar muestreador con top-k y top-p para mayor calidad y velocidad
    llama_sampler* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.3f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    // 6. Obtener referencia al callback Java para streaming
    jclass cls = env->GetObjectClass(thiz);
    jmethodID onTokenMethod = env->GetMethodID(cls, "onTokenGenerated", "(Ljava/lang/String;)V");
    bool has_callback = (onTokenMethod != nullptr);
    if (!has_callback) {
        LOGW("No se encontró método onTokenGenerated, streaming deshabilitado");
        // Limpiar la excepción pendiente de GetMethodID si falló
        env->ExceptionClear();
    }

    std::string response = "";
    llama_batch single_batch = llama_batch_init(1, 0, 1);

    // 7. Generación autoregresiva con streaming
    for (int i = 0; i < max_tokens; i++) {
        if (g_n_past >= n_ctx) {
            LOGW("La ventana de contexto se llenó durante la respuesta.");
            break;
        }

        llama_token new_token = llama_sampler_sample(smpl, g_ctx, -1);

        // Si es token de fin de generación (<|im_end|> o EOS), registrarlo y salir
        if (llama_vocab_is_eog(vocab, new_token)) {
            // Guardamos el token de cierre en el KV cache para cerrar el turno
            single_batch.token[0]     = new_token;
            single_batch.pos[0]       = g_n_past;
            single_batch.n_seq_id[0]  = 1;
            single_batch.seq_id[0][0] = 0;
            single_batch.logits[0]    = false;
            single_batch.n_tokens     = 1;
            llama_decode(g_ctx, single_batch);
            g_n_past++;
            break;
        }

        char piece[128];
        int n_piece = llama_token_to_piece(vocab, new_token, piece, sizeof(piece), 0, false);
        if (n_piece > 0) {
            std::string token_str(piece, n_piece);
            response.append(token_str);

            // Streaming: enviar el token inmediatamente al lado Kotlin
            if (has_callback) {
                jstring token_jstr = env->NewStringUTF(token_str.c_str());
                env->CallVoidMethod(thiz, onTokenMethod, token_jstr);
                env->DeleteLocalRef(token_jstr);
                if (env->ExceptionCheck()) {
                    LOGE("Excepción en callback onTokenGenerated");
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                    has_callback = false;
                }
            }
        }

        // Evaluar el token generado y avanzar la posición del contexto
        single_batch.token[0]     = new_token;
        single_batch.pos[0]       = g_n_past;
        single_batch.n_seq_id[0]  = 1;
        single_batch.seq_id[0][0] = 0;
        single_batch.logits[0]    = true;
        single_batch.n_tokens     = 1;

        if (llama_decode(g_ctx, single_batch) != 0) {
            LOGE("Fallo en decode durante la generación");
            break;
        }
        g_n_past++;
    }

    llama_batch_free(single_batch);
    llama_sampler_free(smpl);
    env->DeleteLocalRef(cls);

    LOGI("Respuesta lista. Contexto total en uso: %d / %d tokens (%.1f%%)",
         g_n_past, n_ctx, (g_n_past * 100.0f) / n_ctx);

    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jboolean JNICALL
JNI_FN(restoreContext)(JNIEnv* env, jobject /* this */, jstring transcript_str) {
    if (!g_model || !g_ctx) {
        return JNI_FALSE;
    }

    const char* transcript_chars = env->GetStringUTFChars(transcript_str, nullptr);
    if (!transcript_chars) {
        return JNI_FALSE;
    }
    std::string transcript(transcript_chars);
    env->ReleaseStringUTFChars(transcript_str, transcript_chars);

    llama_memory_t memory = llama_get_memory(g_ctx);
    if (memory) {
        llama_memory_clear(memory, true);
    }
    g_n_past = 0;

    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    std::vector<llama_token> tokens(transcript.size() + 16);
    int n_tokens = llama_tokenize(
        vocab,
        transcript.c_str(),
        transcript.length(),
        tokens.data(),
        static_cast<int32_t>(tokens.size()),
        true,
        true
    );
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(
            vocab,
            transcript.c_str(),
            transcript.length(),
            tokens.data(),
            static_cast<int32_t>(tokens.size()),
            true,
            true
        );
    }
    if (n_tokens <= 0 || n_tokens > llama_n_ctx(g_ctx)) {
        LOGE("No se pudo restaurar el contexto: %d tokens", n_tokens);
        return JNI_FALSE;
    }
    tokens.resize(n_tokens);

    llama_batch batch = llama_batch_init(n_tokens, 0, 1);
    for (int i = 0; i < n_tokens; ++i) {
        batch.token[i] = tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == n_tokens - 1);
    }
    batch.n_tokens = n_tokens;

    const int decode_result = llama_decode(g_ctx, batch);
    llama_batch_free(batch);
    if (decode_result != 0) {
        LOGE("Fallo al decodificar el contexto restaurado");
        if (memory) {
            llama_memory_clear(memory, true);
        }
        g_n_past = 0;
        return JNI_FALSE;
    }

    g_n_past = n_tokens;
    LOGI("Contexto restaurado: %d tokens", g_n_past);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
JNI_FN(resetContext)(JNIEnv* env, jobject /* this */) {
    if (g_ctx) {
        llama_memory_t mem = llama_get_memory(g_ctx);
        if (mem) {
            llama_memory_clear(mem, true);
        }
        g_n_past = 0;
        LOGI("Contexto reiniciado (KV cache limpiado).");
    }
}

JNIEXPORT void JNICALL
JNI_FN(unloadModel)(JNIEnv* env, jobject /* this */) {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    g_n_past = 0;
    llama_backend_free();
    LOGI("Modelo liberado de memoria.");
}

} // extern "C"