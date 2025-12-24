package com.jankinwu.fntv.client.utils

import oshi.SystemInfo
import java.util.Locale

actual object PlatformInfo {
    private val systemInfo = SystemInfo()
    actual val osName: String = System.getProperty("os.name") ?: "Unknown JVM"
    actual val osArch: String = System.getProperty("os.arch")?.lowercase(Locale.getDefault()) ?: "unknown"
    actual val cpuModel: String = try {
        systemInfo.hardware.processor.processorIdentifier.name.trim()
    } catch (_: Exception) {
        "unknown"
    }
}
