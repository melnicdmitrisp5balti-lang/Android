package com.parentalcontrol.app.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivitySetPasswordBinding

class SetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetPasswordBinding
    private val viewModel: SetPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.set_password)

        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSavePassword.setOnClickListener {
            val password = binding.etNewPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()
            viewModel.setPassword(password, confirm)
        }
    }

    private fun observeViewModel() {
        viewModel.result.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (message == getString(R.string.password_saved)) finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
