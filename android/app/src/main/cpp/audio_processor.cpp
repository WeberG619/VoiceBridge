#include <jni.h>
#include <vector>
#include <android/log.h>
#include <cmath>

#define LOG_TAG "AudioProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

class AudioProcessor {
public:
    static std::vector<float> convertPCMToFloat(const std::vector<short>& pcm) {
        std::vector<float> floatData;
        floatData.reserve(pcm.size());
        
        for (short sample : pcm) {
            floatData.push_back(static_cast<float>(sample) / 32768.0f);
        }
        
        return floatData;
    }
    
    static std::vector<float> applyPreemphasis(const std::vector<float>& audio, float alpha = 0.97f) {
        if (audio.empty()) return audio;
        
        std::vector<float> result;
        result.reserve(audio.size());
        
        result.push_back(audio[0]);
        for (size_t i = 1; i < audio.size(); ++i) {
            result.push_back(audio[i] - alpha * audio[i - 1]);
        }
        
        return result;
    }
    
    static std::vector<float> normalizeAudio(const std::vector<float>& audio) {
        if (audio.empty()) return audio;
        
        float maxVal = 0.0f;
        for (float sample : audio) {
            maxVal = std::max(maxVal, std::abs(sample));
        }
        
        if (maxVal == 0.0f) return audio;
        
        std::vector<float> normalized;
        normalized.reserve(audio.size());
        
        for (float sample : audio) {
            normalized.push_back(sample / maxVal);
        }
        
        return normalized;
    }
    
    static bool detectVoiceActivity(const std::vector<float>& audio, float threshold = 0.02f) {
        if (audio.empty()) return false;
        
        float energy = 0.0f;
        for (float sample : audio) {
            energy += sample * sample;
        }
        energy /= audio.size();
        
        return energy > threshold;
    }
};

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_voicebridge_AudioProcessor_convertPCMToFloat(JNIEnv *env, jobject thiz, jshortArray pcm_data) {
    jsize len = env->GetArrayLength(pcm_data);
    jshort* data = env->GetShortArrayElements(pcm_data, nullptr);
    
    std::vector<short> pcm(data, data + len);
    std::vector<float> floatData = AudioProcessor::convertPCMToFloat(pcm);
    
    env->ReleaseShortArrayElements(pcm_data, data, 0);
    
    jfloatArray result = env->NewFloatArray(floatData.size());
    env->SetFloatArrayRegion(result, 0, floatData.size(), floatData.data());
    
    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_voicebridge_AudioProcessor_normalizeAudio(JNIEnv *env, jobject thiz, jfloatArray audio_data) {
    jsize len = env->GetArrayLength(audio_data);
    jfloat* data = env->GetFloatArrayElements(audio_data, nullptr);
    
    std::vector<float> audio(data, data + len);
    std::vector<float> normalized = AudioProcessor::normalizeAudio(audio);
    
    env->ReleaseFloatArrayElements(audio_data, data, 0);
    
    jfloatArray result = env->NewFloatArray(normalized.size());
    env->SetFloatArrayRegion(result, 0, normalized.size(), normalized.data());
    
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_voicebridge_AudioProcessor_detectVoiceActivity(JNIEnv *env, jobject thiz, jfloatArray audio_data) {
    jsize len = env->GetArrayLength(audio_data);
    jfloat* data = env->GetFloatArrayElements(audio_data, nullptr);
    
    std::vector<float> audio(data, data + len);
    bool hasVoice = AudioProcessor::detectVoiceActivity(audio);
    
    env->ReleaseFloatArrayElements(audio_data, data, 0);
    
    return hasVoice;
}