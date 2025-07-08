package com.voicebridge.crash

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voicebridge.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debug activity to view crash reports
 * Only accessible through developer options
 */
class CrashReportActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CrashReportAdapter
    private lateinit var crashReporter: CrashReporter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout programmatically since this is a debug feature
        recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        setContentView(recyclerView)
        
        supportActionBar?.apply {
            title = "Crash Reports"
            setDisplayHomeAsUpEnabled(true)
        }
        
        crashReporter = CrashReporter.getInstance(this)
        adapter = CrashReportAdapter { report ->
            showCrashDetails(report)
        }
        recyclerView.adapter = adapter
        
        loadCrashReports()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Clear All")
        menu.add(0, 2, 0, "Export")
        menu.add(0, 3, 0, "Test Crash")
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            1 -> {
                clearAllReports()
                true
            }
            2 -> {
                exportReports()
                true
            }
            3 -> {
                testCrash()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun loadCrashReports() {
        lifecycleScope.launch {
            val reports = withContext(Dispatchers.IO) {
                crashReporter.getAllCrashReports()
            }
            adapter.updateReports(reports)
            
            val stats = crashReporter.getCrashStats()
            supportActionBar?.subtitle = "${stats.reportCount} reports"
        }
    }
    
    private fun showCrashDetails(report: CrashReporter.CrashReport) {
        val details = buildString {
            appendLine("Crash ID: ${report.crashId}")
            appendLine("Time: ${report.timestamp}")
            appendLine("App Version: ${report.appVersion}")
            appendLine("Android: ${report.androidVersion}")
            appendLine("Device: ${report.deviceModel}")
            appendLine("Thread: ${report.threadName}")
            appendLine("Exception: ${report.exceptionType}")
            appendLine("Message: ${report.exceptionMessage}")
            appendLine("Memory: ${report.availableMemory / 1024 / 1024}MB available")
            appendLine("Storage: ${report.freeStorage / 1024 / 1024}MB free")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(report.stackTrace)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Crash Report Details")
            .setMessage(details)
            .setPositiveButton("Share") { _, _ ->
                shareReport(report, details)
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun shareReport(report: CrashReporter.CrashReport, details: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VoiceBridge Crash Report ${report.crashId}")
            putExtra(Intent.EXTRA_TEXT, details)
        }
        startActivity(Intent.createChooser(intent, "Share Crash Report"))
    }
    
    private fun clearAllReports() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Reports")
            .setMessage("Are you sure you want to delete all crash reports?")
            .setPositiveButton("Clear") { _, _ ->
                crashReporter.clearCrashReports()
                loadCrashReports()
                Toast.makeText(this, "All crash reports cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportReports() {
        lifecycleScope.launch {
            val reports = withContext(Dispatchers.IO) {
                crashReporter.getAllCrashReports()
            }
            
            val exportText = buildString {
                appendLine("VoiceBridge Crash Reports Export")
                appendLine("Generated: ${System.currentTimeMillis()}")
                appendLine("Total Reports: ${reports.size}")
                appendLine("=" .repeat(50))
                appendLine()
                
                reports.forEach { report ->
                    appendLine("Crash ID: ${report.crashId}")
                    appendLine("Time: ${report.timestamp}")
                    appendLine("Exception: ${report.exceptionType} - ${report.exceptionMessage}")
                    appendLine("Stack Trace:")
                    appendLine(report.stackTrace)
                    appendLine("-".repeat(50))
                    appendLine()
                }
            }
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "VoiceBridge Crash Reports Export")
                putExtra(Intent.EXTRA_TEXT, exportText)
            }
            startActivity(Intent.createChooser(intent, "Export Crash Reports"))
        }
    }
    
    private fun testCrash() {
        AlertDialog.Builder(this)
            .setTitle("Test Crash")
            .setMessage("This will intentionally crash the app for testing purposes.")
            .setPositiveButton("Crash Now") { _, _ ->
                throw RuntimeException("Test crash from CrashReportActivity")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}