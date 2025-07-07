package com.voicebridge

class VoiceBridgeNative {
    
    companion object {
        init {
            System.loadLibrary("voicebridge")
        }
    }
    
    external fun initializeWhisper(modelPath: String): Boolean
    external fun initializeLLaMA(modelPath: String): Boolean
    external fun transcribeAudio(audioData: FloatArray): String
    external fun processText(inputText: String): String
}

class AudioProcessor {
    
    companion object {
        init {
            System.loadLibrary("voicebridge")
        }
    }
    
    external fun convertPCMToFloat(pcmData: ShortArray): FloatArray
    external fun normalizeAudio(audioData: FloatArray): FloatArray
    external fun detectVoiceActivity(audioData: FloatArray): Boolean
}

class TextProcessor {
    
    companion object {
        init {
            System.loadLibrary("voicebridge")
        }
    }
    
    external fun cleanText(inputText: String): String
    external fun extractCommands(inputText: String): Array<String>
    external fun formatForForm(inputText: String, fieldType: String): String
}