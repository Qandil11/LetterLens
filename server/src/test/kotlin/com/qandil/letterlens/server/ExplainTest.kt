package com.qandil.letterlens.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ExplainTest {
    @Test
    fun explain_ok() = testApplication {
        application { module() }

        val http = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val resp = http.post("/explain") {
            contentType(ContentType.Application.Json)
            setBody(ExplainReq("HMRC letter regarding tax return", null))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
    }
}
