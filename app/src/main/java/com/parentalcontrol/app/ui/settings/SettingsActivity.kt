package com.parentalcontrol.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivitySettingsBinding
import com.parentalcontrol.app.ui.auth.SetPasswordActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(this, SetPasswordActivity::class.java))
        }

        binding.btnClearLogs.setOnClickListener {
            viewModel.clearActivityLogs()
        }
    }

    private fun observeViewModel() {
        viewModel.message.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.passwordStatus.observe(this) { status ->
            binding.tvPasswordStatus.text = status
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPasswordStatus()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
