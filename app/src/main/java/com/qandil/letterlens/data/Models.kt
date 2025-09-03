package com.qandil.letterlens.data

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class ExplainReq(val text: String, val hint: String? = null)

@kotlinx.serialization.Serializable
data class ExplainRes(
    val type: String,
    val deadline: String? = null,
    val summary: String,
    val actions: List<String>,
    val citations: List<String>,
)
