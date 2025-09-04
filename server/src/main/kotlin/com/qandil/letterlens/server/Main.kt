package com.qandil.letterlens.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

// Ktor will read configuration from application.conf
fun main(args: Array<String>) = EngineMain.main(args)

// --- types, request/response -----------------------------------------------
@Serializable
data class ExplainReq(val text: String, val hint: String? = null)

private val log: Logger = LoggerFactory.getLogger("LetterLensClassifier")

@Serializable
data class ExplainRes(
    val type: String,
    val deadline: String? = null,
    val summary: String,
    val actions: List<String>,
    val citations: List<String>,
)

// --- routing ----------------------------------------------------------------
fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Companion.Post); allowMethod(HttpMethod.Companion.Get)
    }
    install(CallLogging) { level = Level.INFO }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            this@module.log.error("Unhandled", cause)
            call.respond(HttpStatusCode.Companion.InternalServerError, mapOf("error" to (cause.message ?: "error")))
        }
    }
    routing {
        get("/health") { call.respond(mapOf("ok" to true)) }

        post("/explain") {
            val req = call.receive<ExplainReq>()
            val type = classify(req.text, req.hint)
            val deadline = extractDeadline(req.text, type)
            val (summary, actions, citations) = explainForType(type, req.text)
            call.respond(ExplainRes(type, deadline, summary, actions, citations))
        }
    }
}

// ---------- OCR-tolerant helpers ----------
private fun norm(s: String) = s
    .lowercase()
    .replace('’', '\'')
    .replace('–', '-')
    .replace(Regex("\\s+"), " ")
    .trim()

private fun squash(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

// Build a fuzzy regex for a token so "N H-S" still matches "NHS"
private fun fuzzyRegex(token: String): Regex {
    val letters = token.lowercase().filter { it.isLetterOrDigit() }
    val pattern = letters.map { Regex.escape(it.toString()) }.joinToString("\\W*")
    return Regex(pattern, RegexOption.IGNORE_CASE)
}

private fun hasAny(hay: String, vararg tokens: String): Boolean {
    val low = hay.lowercase()
    val flat = squash(hay)
    return tokens.any { t ->
        low.contains(t.lowercase()) ||
                flat.contains(squash(t)) ||
                fuzzyRegex(t).containsMatchIn(low)
    }
}

// ---------- classify ----------
private fun classify(textRaw: String, hint: String?): String {
    val n = norm("${hint ?: ""} $textRaw")

    // NHS appointment / invite (child flu etc.)
    if (
        hasAny(n, "nhs", "nhs scotland", "greater glasgow and clyde", "public health nhs") &&
        hasAny(n, "appointment", "date and time", "please attend", "clinic",
            "immunis", /* immunisation/ immunization */
            "vaccine", "vaccination", "flu vaccine", "chi number", "location:")
    ) return "NHS Appointment"

    // Electoral Register / Annual Canvass (household response)
    if (
        hasAny(
            n,
            "electoral registration office",
            "is the electoral register information correct for this address",
            "annual canvass",
            "household response",
            "unique security code",
            "register to vote",
            "update your household information",
            "include the names and nationalities of everyone who lives at this address",
            "elecreg.co.uk/glasgow",
            "glasgow city council"
        )
    ) return "Electoral Register"

    // Council tax (keep distinct in case the letter is a bill/arrears)
    if (hasAny(n, "council tax", "liability order", "arrears")) return "Council Tax"

    if (hasAny(n, "hmrc", "self assessment", "self-assessment", "tax return", "utr")) return "HMRC"
    if (hasAny(n, "dvla", "v5c", "driving licence", "vehicle tax")) return "DVLA"
    if (hasAny(n, "ukvi", "home office", "visa", "biometric residence", "brp")) return "UKVI"

    return "Unknown"
}

// ---------- deadlines ----------
private val MONTHS =
    "(?:January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)"
private val DATE_DMY_TEXT = Regex("""\b([0-3]?\d)\s+$MONTHS\s+([12]\d{3})\b""", RegexOption.IGNORE_CASE)
private val DATE_DMY_SLASH = Regex("""\b([0-3]?\d)[/\-]([01]?\d)[/\-]([12]\d{3})\b""")

private fun extractDeadline(raw: String, type: String? = null): String? {
    val n = norm(raw)

    if (type == "NHS Appointment") {
        // Prefer the explicit invite details
        Regex("""(?i)\bdate\s*(and time)?\s*[:\-]\s*([^\n\r,]+(?:,\s*\d{1,2}:\d{2})?)""")
            .find(n)?.groupValues?.getOrNull(2)?.trim()?.let { return it }
        Regex("""(?i)\bappointment\s*[:\-]\s*([^\n\r,]+)""")
            .find(n)?.groupValues?.getOrNull(1)?.trim()?.let { return it }
    }

    Regex("""(?i)(respond|reply|return|deadline)\s*(by|:)?\s*([^\n\r]*)""")
        .find(n)?.groupValues?.getOrNull(3)?.let { tail ->
            DATE_DMY_SLASH.find(tail)?.value ?: DATE_DMY_TEXT.find(tail)?.value
        }?.let { return it }

    return DATE_DMY_SLASH.find(n)?.value ?: DATE_DMY_TEXT.find(n)?.value
}

private fun explainForType(
    type: String,
    text: String
): Triple<String, List<String>, List<String>> {

    fun lines(vararg xs: String) = xs.toList()

    return when (type) {

        "Electoral Register" -> {
            val summary =
                "This looks like an Electoral Register household response / annual canvass letter from your council."
            val actions = lines(
                "Visit the website on the letter and enter your unique security code (Part 1 & Part 2).",
                "Confirm or update the names and nationalities of everyone aged 14+ living at the address.",
                "If nothing has changed, submit the confirmation; otherwise update and submit.",
                "If you can’t respond online, contact your council’s Electoral Registration Office."
            )
            val cites = listOf(
                "https://www.gov.uk/register-to-vote",
                "https://www.gov.uk/electoral-register"
            )
            Triple(summary, actions, cites)
        }

        "NHS Appointment" -> {
            val where = Regex("""(?i)\bLocation\s*[:\-]\s*([^\n\r]+)""").find(text)
                ?.groupValues?.getOrNull(1)?.trim()
            val whenStr = Regex("""(?i)\b(Date\s*(and time)?|Appointment)\s*[:\-]\s*([^\n\r]+)""")
                .find(text)?.groupValues?.getOrNull(3)?.trim()

            val summary = buildString {
                append("This looks like an NHS appointment invite (e.g., clinic or vaccination).")
                if (!whenStr.isNullOrBlank()) append(" When: ").append(whenStr).append('.')
                if (!where.isNullOrBlank())  append(" Location: ").append(where).append('.')
            }

            val actions = lines(
                "Add the appointment date/time to your calendar.",
                "Bring any requested documents (e.g., child Red Book).",
                "If you need to change the appointment, use the phone number or website shown.",
                "Arrive a few minutes early and follow any instructions in the letter."
            )
            val cites = listOf("https://www.nhs.uk/nhs-services/appointments-and-bookings/")
            Triple(summary, actions, cites)
        }

        "Council Tax" -> {
            Triple(
                "This appears to be a Council Tax notice.",
                lines(
                    "Read the bill/notice carefully and note any payment dates.",
                    "Follow the contact or payment steps if action is required.",
                    "Keep a record of any reference numbers."
                ),
                listOf("https://www.gov.uk/council-tax")
            )
        }

        "HMRC" -> {
            Triple(
                "This appears to be an HMRC Self Assessment letter.",
                lines(
                    "Check whether you need to file a return or make a payment.",
                    "Follow any steps listed and keep copies.",
                    "Act by the date shown if one is provided."
                ),
                listOf("https://www.gov.uk/self-assessment-tax-returns")
            )
        }

        else -> {
            Triple(
                "This appears to be a government letter. Review the guidance and respond if required.",
                lines(
                    "Read the official guidance.",
                    "Follow steps if applicable.",
                    "Respond by any deadline."
                ),
                listOf("https://www.gov.uk/")
            )
        }
    }
}