package com.voicebridge

  import android.os.Bundle
  import androidx.appcompat.app.AppCompatActivity
  import android.widget.TextView

  class MainActivity : AppCompatActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          val textView = TextView(this)
          textView.text = "VoiceBridge v1.0.0-beta.1 - Test Build"
          textView.textSize = 18f
          textView.setPadding(50, 50, 50, 50)
          setContentView(textView)
      }
  }
