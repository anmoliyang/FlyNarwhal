package com.jankinwu.fntv.client.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class DesktopLogExporter(private val logsDir: File) : LogExporter {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timestampPattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")

    override fun getAvailableLogDates(): List<String> {
        val today = LocalDate.now()
        return (0..2).map { today.minusDays(it.toLong()).format(dateFormatter) }
    }

    override fun exportErrorLogs(
        date: String,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val logFile = File(logsDir, "FlyNarwhal-$date.log")
        if (!logFile.exists()) {
            onError("日志文件不存在: ${logFile.name}")
            return
        }

        val fileDialog = FileDialog(null as Frame?, "选择保存位置", FileDialog.SAVE).apply {
            file = "FlyNarwhal-Error-$date.log"
            setFilenameFilter { _, name -> name.endsWith(".log") }
            isVisible = true
        }

        val directory = fileDialog.directory
        val filename = fileDialog.file

        if (directory == null || filename == null) {
            return
        }

        val targetFile = File(directory, filename)
        val finalTargetFile = if (targetFile.name.endsWith(".log")) targetFile else File(targetFile.parentFile, "${targetFile.name}.log")

        onStart()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                processAndExport(logFile, finalTargetFile)
                withContext(Dispatchers.Main) {
                    onComplete()
                    revealFile(finalTargetFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("导出失败: ${e.message}")
                }
            }
        }
    }

    private fun processAndExport(source: File, target: File) {
        val lines = source.readLines()
        val entries = mutableListOf<String>()
        var currentEntry = StringBuilder()

        for (line in lines) {
            if (timestampPattern.matcher(line).find()) {
                if (currentEntry.isNotEmpty()) {
                    entries.add(currentEntry.toString())
                }
                currentEntry = StringBuilder(line)
            } else {
                currentEntry.append("\n").append(line)
            }
        }
        if (currentEntry.isNotEmpty()) {
            entries.add(currentEntry.toString())
        }

        val errorIndices = entries.indices.filter { entries[it].contains("[ERROR]", ignoreCase = true) }
        val selectedIndices = mutableSetOf<Int>()

        for (errorIdx in errorIndices) {
            val start = (errorIdx - 100).coerceAtLeast(0)
            val end = (errorIdx + 100).coerceAtMost(entries.size - 1)
            for (i in start..end) {
                selectedIndices.add(i)
            }
        }

        val sortedIndices = selectedIndices.sorted()
        target.bufferedWriter().use { writer ->
            for (idx in sortedIndices) {
                writer.write(entries[idx])
                writer.newLine()
            }
        }
    }

    private fun revealFile(file: File) {
        try {
            val os = System.getProperty("os.name").lowercase()
            when {
                os.contains("win") -> {
                    Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", file.absolutePath))
                }
                os.contains("mac") -> {
                    Runtime.getRuntime().exec(arrayOf("open", "-R", file.absolutePath))
                }
                else -> {
                    // For Linux or others, just open the parent directory
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file.parentFile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
