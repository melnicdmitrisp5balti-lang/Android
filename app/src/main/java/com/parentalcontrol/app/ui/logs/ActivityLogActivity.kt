package com.parentalcontrol.app.ui.logs

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityActivityLogBinding

class ActivityLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivityLogBinding
    private val viewModel: ActivityLogViewModel by viewModels {
        ActivityLogViewModelFactory(this)
    }
    private lateinit var adapter: ActivityLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.activity_log)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ActivityLogAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ActivityLogActivity)
            adapter = this@ActivityLogActivity.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.logs.observe(this) { logs ->
            adapter.submitList(logs)
            binding.tvEmptyState.visibility =
                if (logs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
