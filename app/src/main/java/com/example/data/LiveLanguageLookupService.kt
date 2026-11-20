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

/**
 * Performs live keyless translation (MyMemory Public Translation API across all world languages)
 * and live English dictionary/phonetic/example lookups (Free Dictionary API) over HTTPS so user
 * questions like "not sure if you মানে কি" or any custom sentence receive real, accurate answers.
 */
class LiveLanguageLookupService {

    companion object {
        private const val TAG = "LiveLanguageLookup"
        private const val TIMEOUT_MS = 7_500L
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
                if (translated.isBlank() || translated.equals(clean, ignoreCase = true) && fromLangCode != toLangCode) {
                    // Check matches array for a better translation
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
            connectTimeout = 5_000
            readTimeout = 6_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LingoPulseAndroid/1.0")
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
}
