package com.dispensa.app.data.quiz

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Extracts the (question, answer) pairs that the prompt encodes as
 * <details><summary>Mostra risposta</summary>…</details> inside the
 * "Recupero attivo" / "Domande di autoverifica" blocks.
 *
 * The question is the text immediately preceding the <details> block —
 * usually the previous element sibling (a <p> or a list item) within the
 * same section.
 */
object QuizExtractor {

    data class QA(val question: String, val answer: String)

    fun extract(html: String): List<QA> {
        if (html.isBlank()) return emptyList()
        val doc = Jsoup.parse(html)
        val details = doc.select("details")
        val out = mutableListOf<QA>()
        for (d in details) {
            val answer = answerOf(d)
            val question = questionFor(d)
            if (question.isBlank() || answer.isBlank()) continue
            out += QA(question.trim(), answer.trim())
        }
        return out
    }

    private fun answerOf(details: Element): String {
        val clone = details.clone()
        clone.select("summary").remove()
        return clone.text().replace(Regex("\\s+"), " ")
    }

    private fun questionFor(details: Element): String {
        // 1) previous element sibling with non-empty text
        var sib = details.previousElementSibling()
        while (sib != null) {
            val t = sib.text().trim()
            if (t.isNotEmpty() && sib.tagName() !in skipTags) return t
            sib = sib.previousElementSibling()
        }
        // 2) summary text often contains the question for "compact" formats
        val summary = details.selectFirst("summary")?.text()?.trim().orEmpty()
        if (summary.isNotEmpty() &&
            !summary.contains("mostra", ignoreCase = true) &&
            !summary.contains("risposta", ignoreCase = true)
        ) return summary
        // 3) fall back to the parent section's heading
        val sectionHeader = details.closest("section")
            ?.selectFirst("h1, h2, h3, h4")?.text()?.trim().orEmpty()
        return sectionHeader
    }

    private val skipTags = setOf("script", "style", "br")
}
