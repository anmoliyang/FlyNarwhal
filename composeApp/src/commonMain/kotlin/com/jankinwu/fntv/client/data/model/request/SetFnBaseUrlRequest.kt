package com.jankinwu.fntv.client.data.model.request

import com.fasterxml.jackson.annotation.JsonProperty

data class SetFnBaseUrlRequest(
    @get:JsonProperty("baseUrl")
    @param:JsonProperty("baseUrl")
    val baseUrl: String
)