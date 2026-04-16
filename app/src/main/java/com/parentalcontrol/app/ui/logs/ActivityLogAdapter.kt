package com.parentalcontrol.app.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parentalcontrol.app.data.model.ActivityLog
import com.parentalcontrol.app.databinding.ItemActivityLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityLogAdapter : ListAdapter<ActivityLog, ActivityLogAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemActivityLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(log: ActivityLog) {
            binding.tvAction.text = log.action
            binding.tvDescription.text = log.description
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            binding.tvTimestamp.text = sdf.format(Date(log.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActivityLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<ActivityLog>() {
        override fun areItemsTheSame(oldItem: ActivityLog, newItem: ActivityLog) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ActivityLog, newItem: ActivityLog) =
            oldItem == newItem
    }
}
