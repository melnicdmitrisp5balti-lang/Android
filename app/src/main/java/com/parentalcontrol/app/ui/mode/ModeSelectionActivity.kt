package com.parentalcontrol.app.ui.mode

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.databinding.ActivityModeSelectionBinding
import com.parentalcontrol.app.ui.child.ChildMainActivity
import com.parentalcontrol.app.ui.parent.ParentMainActivity
import com.parentalcontrol.app.viewmodel.ModeSelectionViewModel

class ModeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModeSelectionBinding
    private val viewModel: ModeSelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnChildMode.setOnClickListener {
            viewModel.selectChildMode()
            startActivity(Intent(this, ChildMainActivity::class.java))
        }

        binding.btnParentMode.setOnClickListener {
            viewModel.selectParentMode()
            startActivity(Intent(this, ParentMainActivity::class.java))
        }
    }
}
