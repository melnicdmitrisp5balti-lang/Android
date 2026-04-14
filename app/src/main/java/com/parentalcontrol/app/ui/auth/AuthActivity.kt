package com.parentalcontrol.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityAuthBinding
import com.parentalcontrol.app.ui.main.MainActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val password = binding.etPassword.text.toString()
            if (password.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.authenticate(password)
        }

        binding.btnSetPassword.setOnClickListener {
            startActivity(Intent(this, SetPasswordActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.authResult.observe(this) { success ->
            if (success) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.wrong_password), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.noPasswordSet.observe(this) { noPassword ->
            if (noPassword) {
                binding.tvNoPasswordHint.visibility = android.view.View.VISIBLE
            }
        }
    }
}
