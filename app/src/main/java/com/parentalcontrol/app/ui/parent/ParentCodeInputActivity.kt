package com.parentalcontrol.app.ui.parent

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.parentalcontrol.app.R
import com.parentalcontrol.app.cloud.FirebaseConnectionManager
import com.parentalcontrol.app.databinding.ActivityParentCodeInputBinding
import com.parentalcontrol.app.utils.PreferenceManager
import kotlinx.coroutines.launch

/**
 * Beautiful 6-digit code entry screen for the parent.
 *
 * Each digit has its own [EditText] box.  Focus moves automatically as the
 * parent types.  Entering a valid 6-digit code and tapping "Подключиться"
 * looks up the child device in Firebase and, on success, opens [LiveStreamActivity].
 */
class ParentCodeInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentCodeInputBinding
    private lateinit var prefs: PreferenceManager
    private val firebaseManager = FirebaseConnectionManager()

    /** References to the 6 individual digit boxes in order. */
    private val digitBoxes: List<EditText> by lazy {
        listOf(
            binding.etDigit1,
            binding.etDigit2,
            binding.etDigit3,
            binding.etDigit4,
            binding.etDigit5,
            binding.etDigit6
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentCodeInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)

        setupDigitBoxes()
        setupClickListeners()

        // Pre-fill last used code if available
        val lastCode = prefs.getLastConnectionCode().orEmpty()
        if (lastCode.length == 6 && lastCode.all { it.isDigit() }) {
            lastCode.forEachIndexed { i, ch -> digitBoxes[i].setText(ch.toString()) }
        }
    }

    private fun setupDigitBoxes() {
        digitBoxes.forEachIndexed { index, box ->
            // Limit each box to one character
            box.filters = arrayOf(InputFilter.LengthFilter(1))

            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty()) {
                        // Move focus forward
                        if (index < digitBoxes.size - 1) {
                            digitBoxes[index + 1].requestFocus()
                        } else {
                            // Last box filled — auto-connect
                            connectWithEnteredCode()
                        }
                    }
                }
            })

            // Handle back-space to move focus backwards
            box.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL
                    && event.action == android.view.KeyEvent.ACTION_DOWN
                    && box.text.isNullOrEmpty()
                    && index > 0
                ) {
                    digitBoxes[index - 1].requestFocus()
                    digitBoxes[index - 1].text.clear()
                    true
                } else {
                    false
                }
            }
        }

        // Focus the first empty box
        digitBoxes.firstOrNull { it.text.isNullOrEmpty() }?.requestFocus()
            ?: digitBoxes.last().requestFocus()
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener { connectWithEnteredCode() }

        binding.btnClearCode.setOnClickListener {
            digitBoxes.forEach { it.text.clear() }
            digitBoxes.first().requestFocus()
            setStatus(getString(R.string.enter_6_digit_code), isError = false)
        }
    }

    private fun collectCode(): String =
        digitBoxes.joinToString("") { it.text.toString().trim() }

    private fun connectWithEnteredCode() {
        val code = collectCode()
        if (code.length != 6 || !code.all { it.isDigit() }) {
            setStatus(getString(R.string.code_must_be_6_digits), isError = true)
            return
        }

        setStatus(getString(R.string.searching_device), isError = false)
        binding.btnConnect.isEnabled = false

        lifecycleScope.launch {
            val deviceInfo = firebaseManager.findDevice(code)
            if (deviceInfo != null) {
                prefs.saveLastConnectionCode(code)
                prefs.saveLastChildHost("")  // cloud connection — no LAN host
                setStatus(
                    getString(R.string.device_found, deviceInfo.deviceName),
                    isError = false
                )
                // Open the live stream screen
                startActivity(
                    Intent(this@ParentCodeInputActivity, LiveStreamActivity::class.java).apply {
                        putExtra(LiveStreamActivity.EXTRA_CODE, code)
                        putExtra(LiveStreamActivity.EXTRA_CHILD_NAME, deviceInfo.deviceName)
                    }
                )
            } else {
                setStatus(getString(R.string.device_not_found), isError = true)
                binding.btnConnect.isEnabled = true
            }
        }
    }

    private fun setStatus(message: String, isError: Boolean) {
        binding.tvConnectionStatus.text = message
        binding.tvConnectionStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (isError) R.color.neon_magenta else R.color.neon_green
            )
        )
    }
}
