package com.parentalcontrol.app.ui.parent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.databinding.ActivityAudioStreamBinding

class AudioStreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioStreamBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
