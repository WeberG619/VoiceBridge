#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#ifdef WHISPER_AVAILABLE
#include "whisper.h"
#endif

#ifdef LLAMA_AVAILABLE
#include "llama.h"
#endif

#define LOG_TAG "VoiceBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

class VoiceBridge {
private:
#ifdef WHISPER_AVAILABLE
    whisper_context* whisper_ctx = nullptr;
#endif
#ifdef LLAMA_AVAILABLE
    llama_context* llama_ctx = nullptr;
#endif

public:
    bool initializeWhisper(const std::string& modelPath) {
#ifdef WHISPER_AVAILABLE
        whisper_ctx = whisper_init_from_file(modelPath.c_str());
        if (!whisper_ctx) {
            LOGE("Failed to initialize Whisper model");
            return false;
        }
        LOGI("Whisper model initialized successfully");
        return true;
#else
        LOGE("Whisper not available");
        return false;
#endif
    }

    bool initializeLLaMA(const std::string& modelPath) {
#ifdef LLAMA_AVAILABLE
        llama_backend_init(false);
        llama_model_params model_params = llama_model_default_params();
        llama_model* model = llama_load_model_from_file(modelPath.c_str(), model_params);
        
        if (!model) {
            LOGE("Failed to load LLaMA model");
            return false;
        }
        
        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = 2048;
        ctx_params.n_threads = 4;
        
        llama_ctx = llama_new_context_with_model(model, ctx_params);
        if (!llama_ctx) {
            LOGE("Failed to create LLaMA context");
            return false;
        }
        
        LOGI("LLaMA model initialized successfully");
        return true;
#else
        LOGE("LLaMA not available");
        return false;
#endif
    }

    std::string transcribeAudio(const std::vector<float>& audio) {
#ifdef WHISPER_AVAILABLE
        if (!whisper_ctx) {
            LOGE("Whisper not initialized");
            return "";
        }
        
        whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.print_realtime = false;
        params.print_progress = false;
        
        if (whisper_full(whisper_ctx, params, audio.data(), audio.size()) != 0) {
            LOGE("Failed to process audio");
            return "";
        }
        
        std::string result;
        const int n_segments = whisper_full_n_segments(whisper_ctx);
        for (int i = 0; i < n_segments; ++i) {
            const char* text = whisper_full_get_segment_text(whisper_ctx, i);
            result += text;
        }
        
        return result;
#else
        LOGE("Whisper not available");
        return "";
#endif
    }

    std::string processText(const std::string& input) {
#ifdef LLAMA_AVAILABLE
        if (!llama_ctx) {
            LOGE("LLaMA not initialized");
            return "";
        }
        
        // Simplified text processing - in real implementation, 
        // this would be more sophisticated
        return "Processed: " + input;
#else
        LOGE("LLaMA not available");
        return "";
#endif
    }

    ~VoiceBridge() {
#ifdef WHISPER_AVAILABLE
        if (whisper_ctx) {
            whisper_free(whisper_ctx);
        }
#endif
#ifdef LLAMA_AVAILABLE
        if (llama_ctx) {
            llama_free(llama_ctx);
        }
        llama_backend_free();
#endif
    }
};

static VoiceBridge* g_voicebridge = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_voicebridge_VoiceBridgeNative_initializeWhisper(JNIEnv *env, jobject thiz, jstring model_path) {
    if (!g_voicebridge) {
        g_voicebridge = new VoiceBridge();
    }
    
    const char* path = env->GetStringUTFChars(model_path, 0);
    bool result = g_voicebridge->initializeWhisper(std::string(path));
    env->ReleaseStringUTFChars(model_path, path);
    
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_voicebridge_VoiceBridgeNative_initializeLLaMA(JNIEnv *env, jobject thiz, jstring model_path) {
    if (!g_voicebridge) {
        g_voicebridge = new VoiceBridge();
    }
    
    const char* path = env->GetStringUTFChars(model_path, 0);
    bool result = g_voicebridge->initializeLLaMA(std::string(path));
    env->ReleaseStringUTFChars(model_path, path);
    
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voicebridge_VoiceBridgeNative_transcribeAudio(JNIEnv *env, jobject thiz, jfloatArray audio_data) {
    if (!g_voicebridge) {
        return env->NewStringUTF("");
    }
    
    jsize len = env->GetArrayLength(audio_data);
    jfloat* data = env->GetFloatArrayElements(audio_data, nullptr);
    
    std::vector<float> audio(data, data + len);
    std::string result = g_voicebridge->transcribeAudio(audio);
    
    env->ReleaseFloatArrayElements(audio_data, data, 0);
    
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voicebridge_VoiceBridgeNative_processText(JNIEnv *env, jobject thiz, jstring input_text) {
    if (!g_voicebridge) {
        return env->NewStringUTF("");
    }
    
    const char* text = env->GetStringUTFChars(input_text, 0);
    std::string result = g_voicebridge->processText(std::string(text));
    env->ReleaseStringUTFChars(input_text, text);
    
    return env->NewStringUTF(result.c_str());
}