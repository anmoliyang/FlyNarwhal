package com.jankinwu.fntv.client.utils

import androidx.compose.ui.window.FrameWindowScope
import java.awt.FileDialog
import java.io.File

fun chooseFile(
    scope: FrameWindowScope,
    fileExtensions: Array<String>,
    description: String
): File? {
    return scope.selectFile(fileExtensions, description)
}

fun FrameWindowScope.selectFile(
    fileExtensions: Array<String> = arrayOf("*"),
    description: String = "选择文件"
): File? {
    val fileDialog = FileDialog(this.window, description, FileDialog.LOAD).apply {
        if (fileExtensions.isNotEmpty() && fileExtensions[0] != "*") {
            setFilenameFilter { _, name ->
                fileExtensions.any { ext -> name.endsWith(ext, ignoreCase = true) }
            }
        }
        isVisible = true
    }

    val directory = fileDialog.directory
    val filename = fileDialog.file

    return if (directory != null && filename != null) {
        File(directory, filename)
    } else {
        null
    }
}