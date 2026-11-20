package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TranslationLookupResult(
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val alternatives: List<String> = emptyList()
)

data class DictionaryEntryResult(
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val synonyms: List<String>
)

data class KnowledgeLookupResult(
    val title: String,
    val description: String,
    val summary: String,
    val langCode: String,
    val relatedTitles: List<String> = emptyList(),
    val sourceLabel: String = "Wikipedia"
)

/**
 * Performs live keyless online lookups over HTTPS:
 * 1. Multilingual Translation (MyMemory Public Translation API across all world languages)
 * 2. English Dictionary, Phonetics & Examples (Free Dictionary API)
 * 3. General Knowledge & Encyclopedia Info (Wikipedia Search/Summary API in Bengali, English & world languages + DuckDuckGo Instant Answer API)
 */
class LiveLanguageLookupService {

    companion object {
        private const val TAG = "LiveLanguageLookup"
        private const val TIMEOUT_MS = 8_000L
    }

    suspend fun translate(
        text: String,
        fromLangCode: String,
        toLangCode: String
    ): TranslationLookupResult? = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty() || fromLangCode.equals(toLangCode, ignoreCase = true)) return@withContext null

        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val encodedQ = URLEncoder.encode(clean, "UTF-8")
                val pair = "${normalizeCode(fromLangCode)}|${normalizeCode(toLangCode)}"
                val url = URL("https://api.mymemory.translated.net/get?q=$encodedQ&langpair=$pair")
                val raw = fetchJson(url) ?: return@withTimeoutOrNull null
                val json = JSONObject(raw)
                val responseData = json.optJSONObject("responseData") ?: return@withTimeoutOrNull null
                val translated = responseData.optString("translatedText", "").trim()
                if (translated.isBlank() || (translated.equals(clean, ignoreCase = true) && fromLangCode != toLangCode)) {
                    val matches = json.optJSONArray("matches")
                    val alt = extractBestMatch(matches, clean)
                    if (alt != null) {
                        return@withTimeoutOrNull TranslationLookupResult(
                            sourceText = clean,
                            translatedText = alt,
                            sourceLang = fromLangCode,
                            targetLang = toLangCode
                        )
                    }
                }
                if (translated.isBlank()) return@withTimeoutOrNull null

                val alternatives = mutableListOf<String>()
                val matches = json.optJSONArray("matches")
                if (matches != null) {
                    for (i in 0 until minOf(matches.length(), 4)) {
                        val item = matches.optJSONObject(i) ?: continue
                        val candidate = item.optString("translation", "").trim()
                        if (candidate.isNotBlank() &&
                            !candidate.equals(translated, ignoreCase = true) &&
                            !candidate.equals(clean, ignoreCase = true)
                        ) {
                            alternatives.add(candidate)
                        }
                    }
                }

                TranslationLookupResult(
                    sourceText = clean,
                    translatedText = translated,
                    sourceLang = fromLangCode,
                    targetLang = toLangCode,
                    alternatives = alternatives.distinct().take(2)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Translation lookup failed: ${e.message}")
                null
            }
        }
    }

    suspend fun lookupEnglishWord(word: String): DictionaryEntryResult? = withContext(Dispatchers.IO) {
        val cleanWord = word.trim().lowercase().replace(Regex("[^a-z'-]"), "")
        if (cleanWord.length < 2) return@withContext null

        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val encoded = URLEncoder.encode(cleanWord, "UTF-8")
                val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
                val raw = fetchJson(url) ?: return@withTimeoutOrNull null
                val arr = JSONArray(raw)
                if (arr.length() == 0) return@withTimeoutOrNull null
                val first = arr.getJSONObject(0)

                var phonetic = first.optString("phonetic", "").trim()
                if (phonetic.isBlank()) {
                    val phoneticsArr = first.optJSONArray("phonetics")
                    if (phoneticsArr != null) {
                        for (i in 0 until phoneticsArr.length()) {
                            val text = phoneticsArr.optJSONObject(i)?.optString("text", "")?.trim().orEmpty()
                            if (text.isNotBlank()) {
                                phonetic = text
                                break
                            }
                        }
                    }
                }

                val meanings = first.optJSONArray("meanings") ?: return@withTimeoutOrNull null
                if (meanings.length() == 0) return@withTimeoutOrNull null
                val meaningObj = meanings.getJSONObject(0)
                val partOfSpeech = meaningObj.optString("partOfSpeech", "")
                val defs = meaningObj.optJSONArray("definitions") ?: return@withTimeoutOrNull null
                if (defs.length() == 0) return@withTimeoutOrNull null

                val firstDef = defs.getJSONObject(0)
                val definition = firstDef.optString("definition", "").trim()
                var example: String? = firstDef.optString("example", "").trim().takeIf { it.isNotBlank() }
                if (example == null) {
                    for (i in 1 until defs.length()) {
                        val ex = defs.optJSONObject(i)?.optString("example", "")?.trim()
                        if (!ex.isNullOrBlank()) {
                            example = ex
                            break
                        }
                    }
                }

                val synonyms = mutableListOf<String>()
                val synArr = meaningObj.optJSONArray("synonyms")
                if (synArr != null) {
                    for (i in 0 until minOf(synArr.length(), 4)) {
                        val s = synArr.optString(i, "").trim()
                        if (s.isNotBlank()) synonyms.add(s)
                    }
                }

                if (definition.isBlank()) return@withTimeoutOrNull null
                DictionaryEntryResult(
                    word = first.optString("word", cleanWord),
                    phonetic = phonetic,
                    partOfSpeech = partOfSpeech,
                    definition = definition,
                    example = example,
                    synonyms = synonyms
                )
            } catch (e: Exception) {
                Log.w(TAG, "Dictionary lookup failed for '$cleanWord': ${e.message}")
                null
            }
        }
    }

    /**
     * Searches Wikipedia in [primaryLangCode] (e.g. "bn" or "en"), falls back to English Wikipedia
     * (translating the query if needed) and DuckDuckGo Instant Answer API so virtually any
     * general-knowledge question gets a real, accurate summary.
     */
    suspend fun searchKnowledge(
        query: String,
        primaryLangCode: String
    ): KnowledgeLookupResult? = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext null

        withTimeoutOrNull(9_500L) {
            val wikiLang = normalizeWikiLang(primaryLangCode)
            // 1. Try Wikipedia in the query's primary language first
            val primaryHit = searchWikipedia(clean, wikiLang)
            if (primaryHit != null) return@withTimeoutOrNull primaryHit

            // 2. If primary wasn't English, try translating query to English & searching English Wikipedia
            if (wikiLang != "en") {
                val enQuery = translate(clean, primaryLangCode, "en")?.translatedText ?: clean
                val enHit = searchWikipedia(enQuery, "en")
                if (enHit != null) return@withTimeoutOrNull enHit

                val ddgHit = searchDuckDuckGo(enQuery)
                if (ddgHit != null) return@withTimeoutOrNull ddgHit
            } else {
                // 3. Fallback to DuckDuckGo Instant Answers for English queries
                val ddgHit = searchDuckDuckGo(clean)
                if (ddgHit != null) return@withTimeoutOrNull ddgHit
            }

            null
        }
    }

    private fun searchWikipedia(query: String, wikiLang: String): KnowledgeLookupResult? {
        return try {
            val encodedSearch = URLEncoder.encode(query, "UTF-8")
            val searchUrl = URL(
                "https://$wikiLang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedSearch&format=json&utf8=1&srlimit=4"
            )
            val searchRaw = fetchJson(searchUrl) ?: return null
            val searchArr = JSONObject(searchRaw)
                .optJSONObject("query")
                ?.optJSONArray("search") ?: return null
            if (searchArr.length() == 0) return null

            val titles = mutableListOf<String>()
            for (i in 0 until searchArr.length()) {
                val t = searchArr.optJSONObject(i)?.optString("title", "")?.trim().orEmpty()
                if (t.isNotBlank()) titles.add(t)
            }
            if (titles.isEmpty()) return null

            val bestTitle = titles.first()
            val related = titles.drop(1).take(3)
            val encodedTitle = URLEncoder.encode(bestTitle.replace(" ", "_"), "UTF-8")
            val summaryUrl = URL("https://$wikiLang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle")
            val summaryRaw = fetchJson(summaryUrl)

            if (summaryRaw != null) {
                val sumJson = JSONObject(summaryRaw)
                val extract = sumJson.optString("extract", "").trim()
                val description = sumJson.optString("description", "").trim()
                val displayTitle = sumJson.optString("title", bestTitle).trim()
                if (extract.isNotBlank()) {
                    return KnowledgeLookupResult(
                        title = displayTitle,
                        description = description,
                        summary = trimToSentences(extract, maxChars = 460),
                        langCode = wikiLang,
                        relatedTitles = related,
                        sourceLabel = if (wikiLang == "bn") "বাংলা উইকিপিডিয়া" else "Wikipedia ($wikiLang)"
                    )
                }
            }

            // Fallback to clean search snippet if REST summary was empty
            val rawSnippet = searchArr.optJSONObject(0)?.optString("snippet", "").orEmpty()
            val cleanSnippet = rawSnippet.replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").trim()
            if (cleanSnippet.isNotBlank()) {
                KnowledgeLookupResult(
                    title = bestTitle,
                    description = "",
                    summary = cleanSnippet,
                    langCode = wikiLang,
                    relatedTitles = related,
                    sourceLabel = if (wikiLang == "bn") "বাংলা উইকিপিডিয়া" else "Wikipedia ($wikiLang)"
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search failed ($wikiLang) for '$query': ${e.message}")
            null
        }
    }

    private fun searchDuckDuckGo(query: String): KnowledgeLookupResult? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
            val raw = fetchJson(url) ?: return null
            val json = JSONObject(raw)
            val abstractText = json.optString("AbstractText", "").trim()
            val answer = json.optString("Answer", "").trim()
            val definition = json.optString("Definition", "").trim()
            val heading = json.optString("Heading", query).trim().ifBlank { query }
            val text = abstractText.ifBlank { answer.ifBlank { definition } }
            if (text.isBlank()) return null

            KnowledgeLookupResult(
                title = heading,
                description = json.optString("AbstractSource", "Web Knowledge"),
                summary = trimToSentences(text, maxChars = 460),
                langCode = "en",
                relatedTitles = emptyList(),
                sourceLabel = json.optString("AbstractSource", "Instant Knowledge").ifBlank { "Instant Knowledge" }
            )
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo search failed for '$query': ${e.message}")
            null
        }
    }

    private fun trimToSentences(text: String, maxChars: Int): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= maxChars) return clean
        val cut = clean.take(maxChars)
        val lastPeriod = maxOf(cut.lastIndexOf('.'), cut.lastIndexOf('।'))
        return if (lastPeriod > maxChars / 2) cut.substring(0, lastPeriod + 1) else "$cut…"
    }

    private fun extractBestMatch(matches: JSONArray?, original: String): String? {
        if (matches == null) return null
        for (i in 0 until matches.length()) {
            val candidate = matches.optJSONObject(i)?.optString("translation", "")?.trim().orEmpty()
            if (candidate.isNotBlank() && !candidate.equals(original, ignoreCase = true)) {
                return candidate
            }
        }
        return null
    }

    private fun fetchJson(url: URL): String? {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_500
            readTimeout = 6_500
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LingoPulseAndroid/1.1 (Educational Language & Info App)")
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun normalizeCode(code: String): String = when (code.lowercase()) {
        "zh" -> "zh-CN"
        "tl" -> "tl"
        "no" -> "nb"
        else -> code.lowercase()
    }

    private fun normalizeWikiLang(code: String): String = when (code.lowercase()) {
        "zh", "zh-cn" -> "zh"
        "tl", "fil" -> "tl"
        "nb", "no" -> "no"
        else -> code.lowercase().take(2).ifBlank { "en" }
    }
}
