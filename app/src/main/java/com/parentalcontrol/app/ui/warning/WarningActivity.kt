package com.parentalcontrol.app.ui.warning

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.databinding.ActivityWarningBinding
import com.parentalcontrol.app.ui.auth.AuthActivity
import com.parentalcontrol.app.utils.PreferenceManager

class WarningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWarningBinding
    private lateinit var prefs: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)

        if (prefs.isWarningAccepted()) {
            navigateToAuth()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnAccept.setOnClickListener {
            prefs.setWarningAccepted(true)
            navigateToAuth()
        }

        binding.btnDecline.setOnClickListener {
            finish()
        }
    }

    private fun navigateToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
