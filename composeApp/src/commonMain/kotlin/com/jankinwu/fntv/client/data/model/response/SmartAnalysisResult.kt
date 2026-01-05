package com.jankinwu.fntv.client.data.model.response

/**
 * Generic response wrapper for Smart Analysis API.
 */
data class SmartAnalysisResult<T>(
    val code: Int,
    val msg: String,
    val data: T?
) {
    /**
     * Returns true if the request was successful (code 0).
     */
    fun isSuccess(): Boolean = code == 0
}
