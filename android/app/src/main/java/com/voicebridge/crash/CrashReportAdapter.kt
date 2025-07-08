package com.voicebridge.crash

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * RecyclerView adapter for crash reports
 */
class CrashReportAdapter(
    private val onItemClick: (CrashReporter.CrashReport) -> Unit
) : RecyclerView.Adapter<CrashReportAdapter.ViewHolder>() {
    
    private var reports = listOf<CrashReporter.CrashReport>()
    
    fun updateReports(newReports: List<CrashReporter.CrashReport>) {
        reports = newReports
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(Color.WHITE)
            
            // Add bottom border
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 2)
            layoutParams = params
        }
        
        val titleText = TextView(parent.context).apply {
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val subtitleText = TextView(parent.context).apply {
            textSize = 14f
            setTextColor(Color.GRAY)
        }
        
        val timeText = TextView(parent.context).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.END
        }
        
        layout.addView(titleText)
        layout.addView(subtitleText)
        layout.addView(timeText)
        
        return ViewHolder(layout, titleText, subtitleText, timeText)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        
        holder.titleText.text = "${report.exceptionType} (${report.crashId.take(8)})"
        holder.subtitleText.text = "${report.exceptionMessage.take(100)}${if (report.exceptionMessage.length > 100) "..." else ""}"
        
        // Format timestamp
        try {
            val instant = Instant.parse(report.timestamp)
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")
                .withZone(ZoneId.systemDefault())
            holder.timeText.text = formatter.format(instant)
        } catch (e: Exception) {
            holder.timeText.text = report.timestamp
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(report)
        }
        
        // Add ripple effect
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = holder.itemView.context.obtainStyledAttributes(attrs)
        val backgroundResource = typedArray.getResourceId(0, 0)
        holder.itemView.setBackgroundResource(backgroundResource)
        typedArray.recycle()
    }
    
    override fun getItemCount() = reports.size
    
    class ViewHolder(
        itemView: LinearLayout,
        val titleText: TextView,
        val subtitleText: TextView,
        val timeText: TextView
    ) : RecyclerView.ViewHolder(itemView)
}