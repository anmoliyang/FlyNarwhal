package com.jankinwu.fntv.client.utils

import co.touchlab.kermit.Logger
import com.jankinwu.fntv.client.data.model.response.SubtitleStream
import com.jankinwu.fntv.client.data.store.AccountDataCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExternalSubtitleUtil(
    private val client: HttpClient,
    private val subtitleStream: SubtitleStream
) {
    private val logger = Logger.withTag("ExternalSubtitleUtil")
    private val cues = mutableListOf<SubtitleCue>()
    private var isInitialized = false

    suspend fun initialize() {
        if (isInitialized) return
        withContext(Dispatchers.IO) {
            try {
                val subtitleUrl = "${AccountDataCache.getProxyBaseUrl()}/v/api/v1/subtitle/dl/${subtitleStream.guid}"
//                val token = AccountDataCache.getAccessToken()
                
                val response = client.get(subtitleUrl) {
//                    if (!token.isNullOrBlank()) {
//                        header("Authorization", "Bearer $token")
//                    }
                }
                val content = response.bodyAsText()
                
                val parsedCues = when (subtitleStream.format.lowercase()) {
                    "srt" -> parseSrt(content)
                    "ass", "ssa" -> parseAss(content)
                    "vtt" -> parseVtt(content)
                    else -> emptyList()
                }
                
                cues.clear()
                cues.addAll(parsedCues)
                isInitialized = true
                logger.i { "Initialized ExternalSubtitleUtil with ${cues.size} cues (${subtitleStream.format})" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to initialize ExternalSubtitleUtil" }
            }
        }
    }

    fun getCurrentSubtitle(currentPositionMs: Long): String? {
        val activeCues = cues.filter { cue ->
            currentPositionMs >= cue.startTime && currentPositionMs < cue.endTime
        }
        if (activeCues.isEmpty()) return null
        return activeCues.joinToString("\n") { it.text }
    }

    private fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        // Normalize line endings
        val text = content.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = text.split("\n\n")

        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.size >= 3) {
                // Line 0: Index (can be ignored)
                // Line 1: Timecode
                val timeCodeLine = lines.find { it.contains("-->") } ?: continue
                val timeParts = timeCodeLine.split("-->")
                if (timeParts.size != 2) continue

                val startTime = parseSrtTime(timeParts[0].trim())
                val endTime = parseSrtTime(timeParts[1].trim())
                
                // Content starts after timecode line
                val timeCodeIndex = lines.indexOf(timeCodeLine)
                if (timeCodeIndex < 0 || timeCodeIndex >= lines.size - 1) continue
                
                val textLines = lines.subList(timeCodeIndex + 1, lines.size)
                val subtitleText = textLines.joinToString("\n")

                if (subtitleText.isNotBlank()) {
                    cues.add(SubtitleCue(startTime, endTime, subtitleText))
                }
            }
        }
        return cues
    }

    private fun parseSrtTime(timeStr: String): Long {
        // Format: HH:MM:SS,mmm or HH:MM:SS.mmm
        try {
            val parts = timeStr.replace(',', '.').split(':')
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secondsParts = parts[2].split('.')
                val seconds = secondsParts[0].toLong()
                val millis = if (secondsParts.size > 1) secondsParts[1].padEnd(3, '0').take(3).toLong() else 0L
                
                return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + millis
            }
        } catch (e: Exception) {
            // Ignore malformed time
        }
        return 0L
    }

    private fun parseAss(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        var formatIndexMap = mutableMapOf<String, Int>()
        
        // Find [Events] section and Format line
        var inEventsSection = false
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.equals("[Events]", ignoreCase = true)) {
                inEventsSection = true
                continue
            }
            
            if (inEventsSection) {
                if (trimmed.startsWith("Format:", ignoreCase = true)) {
                    val formatLine = trimmed.substringAfter("Format:").trim()
                    val parts = formatLine.split(",").map { it.trim().lowercase() }
                    parts.forEachIndexed { index, name -> formatIndexMap[name] = index }
                } else if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                    val dialogueLine = trimmed.substringAfter("Dialogue:").trim()
                    // ASS CSV is tricky because the last field (Text) can contain commas.
                    // We need to limit the split.
                    val formatCount = formatIndexMap.size
                    if (formatCount > 0) {
                        val parts = dialogueLine.split(",", limit = formatCount).map { it.trim() }
                        if (parts.size == formatCount) {
                            val startIndex = formatIndexMap["start"] ?: -1
                            val endIndex = formatIndexMap["end"] ?: -1
                            val textIndex = formatIndexMap["text"] ?: -1
                            
                            if (startIndex != -1 && endIndex != -1 && textIndex != -1) {
                                val startTime = parseAssTime(parts[startIndex])
                                val endTime = parseAssTime(parts[endIndex])
                                var text = parts[textIndex]
                                
                                // Clean ASS tags (e.g., {\pos(100,100)})
                                text = text.replace(Regex("\\{.*?\\}"), "")
                                text = text.replace("\\N", "\n", ignoreCase = true)
                                text = text.replace("\\n", "\n", ignoreCase = true)
                                
                                if (text.isNotBlank()) {
                                    cues.add(SubtitleCue(startTime, endTime, text))
                                }
                            }
                        }
                    }
                }
            }
        }
        return cues
    }

    private fun parseAssTime(timeStr: String): Long {
        // Format: H:MM:SS.cc (centiseconds)
        try {
            val parts = timeStr.split(':')
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secondsParts = parts[2].split('.')
                val seconds = secondsParts[0].toLong()
                val centis = if (secondsParts.size > 1) secondsParts[1].padEnd(2, '0').take(2).toLong() else 0L
                
                return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + (centis * 10)
            }
        } catch (e: Exception) {
            // Ignore malformed time
        }
        return 0L
    }
    
    private fun parseVtt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                // Found timestamp line
                try {
                    val parts = line.split("-->")
                    val startStr = parts[0].trim()
                    val endStr = parts[1].trim()
                    val startMs = parseSrtTime(startStr) // VTT time format is similar to SRT
                    val endMs = parseSrtTime(endStr)
                    
                    // Collect text
                    val textBuilder = StringBuilder()
                    i++
                    while (i < lines.size && lines[i].trim().isNotBlank()) {
                        textBuilder.append(lines[i].trim()).append("\n")
                        i++
                    }
                    val text = textBuilder.toString().trim()
                    
                    if (text.isNotEmpty()) {
                        cues.add(SubtitleCue(startMs, endMs, text))
                    }
                } catch (e: Exception) {
                    // Ignore malformed
                }
            }
            i++
        }
        return cues
    }
}
