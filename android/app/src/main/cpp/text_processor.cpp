#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <algorithm>
#include <sstream>

#define LOG_TAG "TextProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

class TextProcessor {
public:
    static std::string cleanText(const std::string& input) {
        std::string result = input;
        
        // Remove extra whitespace
        result.erase(std::remove_if(result.begin(), result.end(), 
            [](unsigned char c) { return std::isspace(c) && c != ' '; }), result.end());
        
        // Replace multiple spaces with single space
        std::string::size_type pos = 0;
        while ((pos = result.find("  ", pos)) != std::string::npos) {
            result.replace(pos, 2, " ");
        }
        
        // Trim leading and trailing spaces
        result.erase(0, result.find_first_not_of(' '));
        result.erase(result.find_last_not_of(' ') + 1);
        
        return result;
    }
    
    static std::vector<std::string> extractCommands(const std::string& text) {
        std::vector<std::string> commands;
        std::istringstream iss(text);
        std::string word;
        
        // Simple command extraction - look for action words
        std::vector<std::string> actionWords = {
            "fill", "enter", "select", "click", "tap", "open", "close", "submit"
        };
        
        while (iss >> word) {
            // Convert to lowercase for comparison
            std::string lowerWord = word;
            std::transform(lowerWord.begin(), lowerWord.end(), lowerWord.begin(), ::tolower);
            
            if (std::find(actionWords.begin(), actionWords.end(), lowerWord) != actionWords.end()) {
                commands.push_back(word);
            }
        }
        
        return commands;
    }
    
    static std::string formatForForm(const std::string& text, const std::string& fieldType) {
        std::string formatted = cleanText(text);
        
        if (fieldType == "phone") {
            // Remove non-numeric characters
            formatted.erase(std::remove_if(formatted.begin(), formatted.end(), 
                [](char c) { return !std::isdigit(c); }), formatted.end());
            
            // Format as phone number if 10 digits
            if (formatted.length() == 10) {
                formatted = "(" + formatted.substr(0, 3) + ") " + 
                           formatted.substr(3, 3) + "-" + formatted.substr(6, 4);
            }
        } else if (fieldType == "email") {
            // Convert to lowercase
            std::transform(formatted.begin(), formatted.end(), formatted.begin(), ::tolower);
        } else if (fieldType == "name") {
            // Capitalize first letter of each word
            bool capitalizeNext = true;
            for (char& c : formatted) {
                if (std::isspace(c)) {
                    capitalizeNext = true;
                } else if (capitalizeNext) {
                    c = std::toupper(c);
                    capitalizeNext = false;
                }
            }
        }
        
        return formatted;
    }
};

extern "C" JNIEXPORT jstring JNICALL
Java_com_voicebridge_TextProcessor_cleanText(JNIEnv *env, jobject thiz, jstring input_text) {
    const char* text = env->GetStringUTFChars(input_text, 0);
    std::string cleaned = TextProcessor::cleanText(std::string(text));
    env->ReleaseStringUTFChars(input_text, text);
    
    return env->NewStringUTF(cleaned.c_str());
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_voicebridge_TextProcessor_extractCommands(JNIEnv *env, jobject thiz, jstring input_text) {
    const char* text = env->GetStringUTFChars(input_text, 0);
    std::vector<std::string> commands = TextProcessor::extractCommands(std::string(text));
    env->ReleaseStringUTFChars(input_text, text);
    
    jobjectArray result = env->NewObjectArray(commands.size(), env->FindClass("java/lang/String"), nullptr);
    
    for (size_t i = 0; i < commands.size(); ++i) {
        env->SetObjectArrayElement(result, i, env->NewStringUTF(commands[i].c_str()));
    }
    
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voicebridge_TextProcessor_formatForForm(JNIEnv *env, jobject thiz, jstring input_text, jstring field_type) {
    const char* text = env->GetStringUTFChars(input_text, 0);
    const char* type = env->GetStringUTFChars(field_type, 0);
    
    std::string formatted = TextProcessor::formatForForm(std::string(text), std::string(type));
    
    env->ReleaseStringUTFChars(input_text, text);
    env->ReleaseStringUTFChars(field_type, type);
    
    return env->NewStringUTF(formatted.c_str());
}