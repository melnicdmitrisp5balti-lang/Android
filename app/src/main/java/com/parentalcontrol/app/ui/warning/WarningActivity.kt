package com.parentalcontrol.app.ui.warning

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.databinding.ActivityWarningBinding
import com.parentalcontrol.app.ui.auth.AuthActivity

class WarningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWarningBinding
    private val viewModel: WarningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check early before inflating layout to avoid showing warning screen on subsequent launches
        if (viewModel.isWarningAccepted.value == true) {
            navigateToAuth()
            return
        }

        binding = ActivityWarningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnAccept.setOnClickListener {
            viewModel.acceptWarning()
        }

        binding.btnDecline.setOnClickListener {
            viewModel.declineWarning()
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.isWarningAccepted.observe(this) { accepted ->
            if (accepted) {
                navigateToAuth()
            }
        }
    }

    private fun navigateToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
