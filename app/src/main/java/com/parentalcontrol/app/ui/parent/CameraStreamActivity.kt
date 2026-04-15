package com.parentalcontrol.app.ui.parent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.databinding.ActivityCameraStreamBinding

class CameraStreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraStreamBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
