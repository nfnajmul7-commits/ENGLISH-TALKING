package com.example.data

import com.example.model.LearningModule
import com.example.model.TutorResponse
import com.example.model.UserSettings
import com.example.model.VocabPhrase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AriaQuestionType {
    NONE,
    ASKED_NAME,
    ASKED_ORIGIN,
    ASKED_HOBBY,
    ASKED_DAY,
    ASKED_GOAL,
    ASKED_MOOD,
    ASKED_KNOWLEDGE_MORE
}

data class ConversationMemory(
    var userName: String? = null,
    var userOrigin: String? = null,
    var lastTopic: String? = null,
    var lastKnowledgeTitle: String? = null,
    var lastRelatedTopics: List<String> = emptyList(),
    var lastQuestionType: AriaQuestionType = AriaQuestionType.NONE,
    var lastSimpleSummary: String? = null
)

sealed class CompanionAction {
    data class DirectResponse(val response: TutorResponse) : CompanionAction()
    data class LookupKnowledge(val query: String, val isFollowUpMore: Boolean = false) : CompanionAction()
    data object NotHandled : CompanionAction()
}

/**
 * Provides real-time utilities (date/time, math, unit conversion), multi-turn conversation memory,
 * Bengali + Banglish + English intent understanding, and factual question extraction so Aria
 * truly keeps pace with what the user is saying or asking.
 */
object ConversationalEngine {

    // -------------------------------------------------------------------------
    // 1. REAL-TIME DATE, TIME, MATH & UNIT CONVERSION
    // -------------------------------------------------------------------------

    fun tryAnswerUtility(raw: String, settings: UserSettings): TutorResponse? {
        val isBn = settings.nativeLanguageCode == "bn"
        tryAnswerDateTime(raw, isBn)?.let { return it }
        tryAnswerMathOrUnit(raw, isBn)?.let { return it }
        return null
    }

    private fun tryAnswerDateTime(raw: String, isBn: Boolean): TutorResponse? {
        val lower = raw.lowercase().trim()
        val asksTime = lower.contains("কয়টা বাজে") || lower.contains("সময় কত") || lower.contains("এখন সময়") ||
            lower.contains("koto baje") || lower.contains("somoy koto") || lower.contains("what time") ||
            lower == "time" || lower.contains("current time")
        val asksDate = lower.contains("কত তারিখ") || lower.contains("আজকের তারিখ") || lower.contains("কী বার") ||
            lower.contains("কি বার") || lower.contains("koto tarikh") || lower.contains("ki bar") ||
            lower.contains("today's date") || lower.contains("what date") || lower.contains("what day is")

        if (!asksTime && !asksDate) return null

        val now = Date()
        val timeEn = SimpleDateFormat("h:mm a", Locale.US).format(now)
        val dateEn = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(now)
        val dayBn = when (SimpleDateFormat("EEEE", Locale.US).format(now).lowercase()) {
            "saturday" -> "শনিবার"
            "sunday" -> "রবিবার"
            "monday" -> "সোমবার"
            "tuesday" -> "মঙ্গলবার"
            "wednesday" -> "বুধবার"
            "thursday" -> "বৃহস্পতিবার"
            else -> "শুক্রবার"
        }

        val answerEn = if (asksTime && !asksDate) {
            "Right now it is $timeEn."
        } else if (asksDate && !asksTime) {
            "Today is $dateEn."
        } else {
            "Right now it is $timeEn on $dateEn."
        }

        val answerBn = if (asksTime && !asksDate) {
            "⏰ এখন সময়: $timeEn ($dayBn)।"
        } else if (asksDate && !asksTime) {
            "📅 আজ $dayBn, $dateEn।"
        } else {
            "⏰ এখন $timeEn, এবং আজ $dayBn ($dateEn)।"
        }

        return TutorResponse(
            spokenText = answerEn,
            nativeExplanation = if (isBn) "$answerBn\n\n🇬🇧 ইংরেজিতে: $answerEn" else answerEn,
            phrases = listOf(
                VocabPhrase("What time is it right now?", "হোয়াট টাইম ইজ ইট রাইট নাউ?", if (isBn) "এখন কয়টা বাজে?" else "Ask the current time."),
                VocabPhrase(answerEn, "", if (isBn) "বর্তমান সময় ও তারিখ বলার স্বাভাবিক ইংরেজি।" else "Natural way to state time/date.")
            ),
            practicePrompt = if (isBn) "আরও কোনো তথ্য জানতে চান, নাকি একটু ইংরেজি কথা বলবেন?" else "Need any other info, or want to keep chatting?",
            grammarFeedback = if (isBn) "✅ তাৎক্ষণিক সময় ও তারিখ জানানো হয়েছে।" else "✅ Real-time clock info.",
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("আজ কত তারিখ?", "বাংলাদেশ সম্পর্কে বলো", "25 * 48 কত?", "কুইজ দিন")
            else listOf("What date is today?", "Tell me about Space", "25 * 48", "Quiz me")
        )
    }

    private fun tryAnswerMathOrUnit(raw: String, isBn: Boolean): TutorResponse? {
        val normalized = normalizeBengaliDigits(raw)
            .replace("×", "*")
            .replace("÷", "/")
            .replace("গুণ", "*")
            .replace("ভাগ", "/")
            .replace("যোগ", "+")
            .replace("বিয়োগ", "-")

        // Unit conversion checks (km<->miles, c<->f, kg<->lb, ft<->cm)
        val unitRegex = Regex("""(\d+(?:\.\d+)?)\s*(km|kilometer|kilometers|miles?|c|celsius|f|fahrenheit|kg|kilogram|lb|lbs|pound|pounds|ft|feet|foot|cm)\s*(?:to|in|থেকে|কত|=)\s*(km|miles?|c|celsius|f|fahrenheit|kg|lb|lbs|pounds?|cm|ft|feet)""", RegexOption.IGNORE_CASE)
        unitRegex.find(normalized)?.let { match ->
            val value = match.groupValues[1].toDoubleOrNull() ?: return null
            val from = match.groupValues[2].lowercase()
            val to = match.groupValues[3].lowercase()
            val result: Pair<Double, String>? = when {
                from.startsWith("km") && to.startsWith("mile") -> (value * 0.621371) to "miles"
                from.startsWith("mile") && to.startsWith("km") -> (value * 1.60934) to "km"
                (from == "c" || from == "celsius") && (to == "f" || to == "fahrenheit") -> (value * 9.0 / 5.0 + 32.0) to "°F"
                (from == "f" || from == "fahrenheit") && (to == "c" || to == "celsius") -> ((value - 32.0) * 5.0 / 9.0) to "°C"
                from.startsWith("kg") && (to.startsWith("lb") || to.startsWith("pound")) -> (value * 2.20462) to "lbs"
                (from.startsWith("lb") || from.startsWith("pound")) && to.startsWith("kg") -> (value * 0.453592) to "kg"
                (from == "ft" || from == "feet" || from == "foot") && to == "cm" -> (value * 30.48) to "cm"
                from == "cm" && (to == "ft" || to == "feet") -> (value / 30.48) to "ft"
                else -> null
            }
            if (result != null) {
                val formatted = String.format(Locale.US, "%.2f", result.first).removeSuffix(".00")
                val enLine = "$value $from equals $formatted ${result.second}."
                val bnLine = "🧮 হিসাবের উত্তর: $value $from = $formatted ${result.second}।"
                return TutorResponse(
                    spokenText = enLine,
                    nativeExplanation = if (isBn) "$bnLine\n\n🇬🇧 ইংরেজিতে: $enLine" else enLine,
                    phrases = listOf(VocabPhrase(enLine, "", if (isBn) "ইউনিট কনভার্সন ফলাফল।" else "Unit conversion result.")),
                    practicePrompt = if (isBn) "অন্য কোনো হিসাব বা তথ্য জানতে চান?" else "Want to calculate or look up anything else?",
                    grammarFeedback = if (isBn) "✅ নির্ভুলভাবে হিসাব সম্পন্ন হয়েছে।" else "✅ Calculated accurately.",
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("37 C to F", "10 km to miles", "এখন কয়টা বাজে?")
                    else listOf("37 C to F", "10 km to miles", "What time is it?")
                )
            }
        }

        // Basic arithmetic: "25 * 48", "120 + 350 কত", "100 / 4"
        val mathRegex = Regex("""(-?\d+(?:\.\d+)?)\s*([+\-*/%])\s*(-?\d+(?:\.\d+)?)""")
        val mathMatch = mathRegex.find(normalized) ?: return null
        val a = mathMatch.groupValues[1].toDoubleOrNull() ?: return null
        val op = mathMatch.groupValues[2]
        val b = mathMatch.groupValues[3].toDoubleOrNull() ?: return null
        if (op == "/" && b == 0.0) return null

        val ans = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> a / b
            "%" -> (a * b) / 100.0
            else -> return null
        }
        val aStr = formatNumber(a)
        val bStr = formatNumber(b)
        val ansStr = formatNumber(ans)
        val opWordEn = when (op) {
            "+" -> "plus"
            "-" -> "minus"
            "*" -> "times"
            "/" -> "divided by"
            else -> "percent of"
        }
        val enLine = "$aStr $opWordEn $bStr equals $ansStr."
        val bnLine = "🧮 হিসাবের উত্তর: $aStr $op $bStr = $ansStr।"

        return TutorResponse(
            spokenText = enLine,
            nativeExplanation = if (isBn) "$bnLine\n\n🇬🇧 ইংরেজিতে: $enLine" else enLine,
            phrases = listOf(VocabPhrase("$aStr $op $bStr = $ansStr", enLine, if (isBn) "ইংরেজিতে এভাবে গণিতের উত্তর বলা হয়।" else "How to read this calculation aloud.")),
            practicePrompt = if (isBn) "আরও কোনো অঙ্ক বা যেকোনো প্রশ্ন করতে পারেন!" else "Ask another calculation or any topic!",
            grammarFeedback = if (isBn) "✅ তাৎক্ষণিক ক্যালকুলেশন।" else "✅ Instant calculation.",
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("এখন কয়টা বাজে?", "সৌরজগৎ সম্পর্কে বলো", "কুইজ দিন")
            else listOf("What time is it?", "Tell me about the Solar System", "Quiz me")
        )
    }

    // -------------------------------------------------------------------------
    // 2. MULTI-TURN CONTEXT & BANGLISH / BENGALI / ENGLISH COMPANION
    // -------------------------------------------------------------------------

    fun tryHandleCompanionOrContext(
        raw: String,
        settings: UserSettings,
        memory: ConversationMemory
    ): CompanionAction {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase()
        val isBn = settings.nativeLanguageCode == "bn"
        val nameTag = memory.userName?.let { ", $it" }.orEmpty()

        // A. "Tell me more" / "আরও বলো" / "হ্যাঁ" following a knowledge answer
        val wantsMore = lower in setOf("আরও বলো", "আরো বলো", "আরও জানাও", "আরো জানাও", "aro bolo", "aro janao", "tell me more", "more", "more info") ||
            ((lower in setOf("হ্যাঁ", "হ্যা", "জি", "জী", "yes", "yeah", "ha", "haa", "ok", "okay", "sure")) &&
                memory.lastQuestionType == AriaQuestionType.ASKED_KNOWLEDGE_MORE)
        if (wantsMore && memory.lastKnowledgeTitle != null) {
            val nextTopic = memory.lastRelatedTopics.firstOrNull() ?: memory.lastKnowledgeTitle!!
            memory.lastRelatedTopics = memory.lastRelatedTopics.drop(1)
            return CompanionAction.LookupKnowledge(query = nextTopic, isFollowUpMore = true)
        }

        // B. "I didn't understand" / "বুঝিনি" / "সহজ করে বলো"
        if (lower.contains("বুঝিনি") || lower.contains("বুঝতে পারিনি") || lower.contains("সহজ করে") ||
            lower.contains("bujhini") || lower.contains("bujhte parchi na") || lower.contains("don't understand") ||
            lower.contains("didn't understand") || lower.contains("explain simply")
        ) {
            val prev = memory.lastSimpleSummary ?: memory.lastTopic
            val replyBn = if (prev != null) {
                "কোনো সমস্যা নেই$nameTag! আমি আরও সহজ করে বুঝিয়ে দিচ্ছি:\n\nমূল কথাটা হলো — $prev। ছোট ছোট অংশে ভাগ করে শিখলে খুব সহজেই মনে থাকবে!"
            } else {
                "কোনো সমস্যা নেই$nameTag! আপনি কোন শব্দ বা বিষয়টি বুঝতে পারেননি, শুধু সেটির নাম লিখে দিন — আমি একদম সহজ বাংলায় উদাহরণসহ বুঝিয়ে দেব।"
            }
            val replyEn = "No worries at all! Let me explain it in a simpler way."
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = replyEn,
                    nativeExplanation = if (isBn) "$replyBn\n\n🇬🇧 $replyEn" else replyEn,
                    phrases = listOf(
                        VocabPhrase("Could you explain that more simply?", "কুড ইউ এক্সপ্লেইন দ্যাট মোর সিম্পলি?", if (isBn) "এটা কি আরেকটু সহজ করে বোঝাবেন?" else "Polite way to ask for a simpler explanation.")
                    ),
                    practicePrompt = if (isBn) "এখন কি কিছুটা পরিষ্কার হয়েছে? যেকোনো প্রশ্ন নির্দ্বিধায় করুন!" else "Does that feel clearer now? Ask me anything!",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("হ্যাঁ, বুঝেছি!", "বাক্যে ব্যবহার দেখান", "কুইজ দিন") else listOf("Yes, got it!", "Give an example", "Quiz me")
                )
            )
        }

        // C. User introducing their name in Bengali, Banglish, or English
        val namePatterns = listOf(
            Regex("""(?:আমার নাম|আমি)\s+([\u0980-\u09FFa-zA-Z]{2,20})"""),
            Regex("""(?i)(?:amar nam|amar name|my name is|i am|i'm|call me)\s+([a-zA-Z]{2,20})""")
        )
        for (regex in namePatterns) {
            val match = regex.find(trimmed)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                val blocked = setOf(
                    "ভালো", "খারাপ", "খুশি", "ক্লান্ত", "ইংরেজি", "বাংলা", "ছাত্র", "চাকরি", "এখন", "আজ", "কি", "কী",
                    "fine", "good", "happy", "sad", "tired", "busy", "ready", "sorry", "sure", "here", "going",
                    "learning", "studying", "working", "from", "not", "very", "okay", "ok", "great", "valo", "bhalo", "kemon"
                )
                if (candidate.lowercase() !in blocked && !trimmed.contains("মানে") && !trimmed.contains("?") && !trimmed.contains("কি")) {
                    memory.userName = candidate
                    memory.lastTopic = "introduction"
                    memory.lastQuestionType = AriaQuestionType.ASKED_ORIGIN
                    val enReply = "Nice to meet you, $candidate! I'm Aria. Where are you from, and what do you love doing?"
                    val bnReply = "আপনার সাথে পরিচিত হয়ে খুব ভালো লাগল, $candidate! আমি আরিয়া। আপনি কোথা থেকে বলছেন, আর অবসর সময়ে কী করতে ভালোবাসেন?"
                    return CompanionAction.DirectResponse(
                        TutorResponse(
                            spokenText = enReply,
                            nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                            phrases = listOf(
                                VocabPhrase("My name is $candidate, and I'm from Bangladesh.", "মাই নেম ইজ $candidate...", if (isBn) "ইংরেজিতে নিজের সুন্দর পরিচয় দিতে।" else "A confident self-introduction."),
                                VocabPhrase("Nice to meet you too!", "নাইস টু মিট ইউ টু!", if (isBn) "আপনার সাথে পরিচিত হয়ে আমারও ভালো লাগল!" else "Warm reply to an introduction.")
                            ),
                            practicePrompt = if (isBn) "আপনি কোন শহর বা দেশ থেকে বলছেন? বাংলায় বা ইংরেজিতে বলুন!" else "Where are you joining from?",
                            grammarFeedback = if (isBn) "✅ আপনার নাম ($candidate) মনে রেখেছি!" else "✅ Nice to meet you, $candidate!",
                            isSampleResponse = true,
                            quickReplies = if (isBn) listOf("আমি ঢাকা থেকে বলছি", "I love cricket and tech", "যেকোনো তথ্য জানতে চাই")
                            else listOf("I'm from Dhaka", "I love tech and travel", "Tell me an interesting fact")
                        )
                    )
                }
            }
        }

        // D. Who is Aria / What can you do?
        if (lower.contains("তুমি কে") || lower.contains("তোমার নাম") || lower.contains("tumi ke") ||
            lower.contains("tomar nam") || lower.contains("who are you") || lower.contains("what is your name") ||
            lower.contains("তুমি কি করতে পারো") || lower.contains("what can you do")
        ) {
            memory.lastTopic = "about_aria"
            memory.lastQuestionType = AriaQuestionType.ASKED_GOAL
            val enReply = "I'm Aria, your friendly language tutor and knowledge companion! I can chat with you, answer facts on any topic, translate phrases live, check your grammar, and quiz you."
            val bnReply = "আমি আরিয়া — আপনার বন্ধু, ভাষা শিক্ষক এবং তথ্য সহায়ক! আপনি আমার সাথে যেকোনো বিষয়ে গল্প করতে পারেন, যেকোনো তথ্য জানতে চাইতে পারেন (বিজ্ঞান, ইতিহাস, দেশ, গণিত), শব্দের অর্থ খুঁজতে পারেন এবং ইংরেজি প্র্যাকটিস করতে পারেন।"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("Can you help me practice English?", "ক্যান ইউ হেল্প মি প্র্যাকটিস ইংলিশ?", if (isBn) "তুমি কি আমাকে ইংরেজি প্র্যাকটিস করতে সাহায্য করবে?" else "Ask for practice help."),
                        VocabPhrase("Tell me something interesting today.", "টেল মি সামথিং ইন্টারেস্টিং টুডে।", if (isBn) "আজ আমাকে মজার কোনো তথ্য বলো।" else "Ask for an interesting fact.")
                    ),
                    practicePrompt = if (isBn) "বলুন, আজ আপনি কী নিয়ে কথা বলতে বা জানতে চান?" else "What would you like to talk about or explore today?",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("বাংলাদেশ সম্পর্কে বলো", "এখন কয়টা বাজে?", "not sure if you মানে কি", "কুইজ দিন")
                    else listOf("Tell me about Earth", "What time is it?", "Teach Past Tense", "Quiz me")
                )
            )
        }

        // E. Asking how Aria is (Bengali, Banglish, English)
        if (lower.contains("কেমন আছো") || lower.contains("কেমন আছেন") || lower.contains("kemon acho") ||
            lower.contains("kemon achen") || lower.contains("kmn acho") || lower.contains("how are you") ||
            lower.contains("how's it going") || lower.contains("how are u")
        ) {
            memory.lastTopic = "wellbeing"
            memory.lastQuestionType = AriaQuestionType.ASKED_DAY
            val enReply = "I'm doing great$nameTag, thank you for asking! How is your day going so far?"
            val bnReply = "আমি খুব ভালো আছি$nameTag, জিজ্ঞেস করার জন্য ধন্যবাদ! আপনার আজকের দিনটা কেমন কাটছে?"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("I'm doing really well today, thanks!", "আইম ডুইং রিয়েলি ওয়েল টুডে, থ্যাংকস!", if (isBn) "আমার আজকের দিনটা খুব ভালো যাচ্ছে, ধন্যবাদ!" else "Natural cheerful reply."),
                        VocabPhrase("A little busy, but everything is fine.", "আ লিটল বিজি, বাট এভরিথিং ইজ ফাইন।", if (isBn) "একটু ব্যস্ত আছি, তবে সবকিছু ভালো চলছে।" else "Realistic everyday reply.")
                    ),
                    practicePrompt = if (isBn) "আপনার দিন কেমন যাচ্ছে বলুন — বাংলায় বা ইংরেজিতে যেভাবে খুশি!" else "Tell me how your day has been!",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("আমি ভালো আছি", "একটু ব্যস্ত ছিলাম", "মনটা একটু খারাপ", "কুইজ দিন")
                    else listOf("I'm doing great!", "A little busy today", "Feeling tired", "Quiz me")
                )
            )
        }

        // F. "What are you doing?" / "কি করছো" / "ki korcho"
        if (lower.contains("কি করছো") || lower.contains("কী করছো") || lower.contains("কি করছেন") ||
            lower.contains("ki korcho") || lower.contains("ki korchen") || lower.contains("what are you doing")
        ) {
            memory.lastQuestionType = AriaQuestionType.ASKED_DAY
            val enReply = "Just here chatting with you$nameTag and ready to explore any topic or practice English together! What are you up to right now?"
            val bnReply = "এই তো$nameTag, তোমার সাথে গল্প করছি আর তুমি যা জানতে চাও তার উত্তর দেওয়ার জন্য তৈরি আছি! তুমি এখন কী করছো?"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("What are you up to right now?", "হোয়াট আর ইউ আপ টু রাইট নাউ?", if (isBn) "তুমি এখন কী করছো? (বন্ধুত্বপূর্ণ ইংরেজি)" else "Casual way to ask 'what are you doing?'."),
                        VocabPhrase("I'm just relaxing and learning something new.", "আইম জাস্ট রিল্যাক্সিং অ্যান্ড লার্নিং...", if (isBn) "আমি বিশ্রাম নিচ্ছি আর নতুন কিছু শিখছি।" else "Relaxed everyday answer.")
                    ),
                    practicePrompt = if (isBn) "তুমি এখন কী করছো বা কোন বিষয় নিয়ে গল্প করতে চাও?" else "What are you up to right now?",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("আমি ইংরেজি শিখছি", "বিশ্রাম নিচ্ছি", "কোনো মজার তথ্য বলো")
                    else listOf("I'm practicing English", "Just relaxing", "Tell me about Space")
                )
            )
        }

        // G. Emotional Empathy (Sad / Tired / Stressed / Bored / Happy)
        val isSadOrStressed = lower.contains("মন খারাপ") || lower.contains("ভালো লাগছে না") || lower.contains("ভালো নেই") ||
            lower.contains("ক্লান্ত") || lower.contains("চিন্তায় আছি") || lower.contains("টেনশন") ||
            lower.contains("mon kharap") || lower.contains("valo lagche na") || lower.contains("klanto") ||
            lower.contains("i am sad") || lower.contains("i'm sad") || lower.contains("feeling down") ||
            lower.contains("stressed") || lower.contains("tired") || lower.contains("bored") || lower.contains("lonely")
        if (isSadOrStressed) {
            memory.lastTopic = "feelings"
            memory.lastQuestionType = AriaQuestionType.ASKED_MOOD
            val enReply = "I'm sorry you're feeling that way$nameTag. Take a deep breath — I'm right here to listen. Want to talk about what's on your mind, or distract yourself with something fun?"
            val bnReply = "শুনে খারাপ লাগল$nameTag। একটু লম্বা শ্বাস নিন, সবকিছু ঠিক হয়ে যাবে। আমি আপনার কথা শুনছি — কী নিয়ে মন খারাপ বা ক্লান্ত লাগছে, চাইলে শেয়ার করতে পারেন, অথবা মন ভালো করতে মজার কোনো গল্প/কুইজ করতে পারি।"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("I've had a long day and feel a bit drained.", "আইভ হ্যাড আ লং ডে...", if (isBn) "আজকের দিনটা অনেক দীর্ঘ ছিল, একটু ক্লান্ত লাগছে।" else "Express feeling tired naturally."),
                        VocabPhrase("Talking about it makes me feel better.", "টকিং অ্যাবাউট ইট মেকস মি ফিল বেটার।", if (isBn) "কথা বললে মনটা হালকা লাগে।" else "Share relief through conversation.")
                    ),
                    practicePrompt = if (isBn) "বলুন তো, আপনার মন ভালো করতে আমি কীভাবে সাহায্য করতে পারি?" else "How can I help brighten your mood right now?",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("মজার কুইজ খেলি", "মহাকাশ সম্পর্কে বলো", "ধন্যবাদ, এখন ভালো লাগছে")
                    else listOf("Let's play a fun quiz", "Tell me about Space", "Thanks, feeling better")
                )
            )
        }

        val isHappyOrFine = lower.contains("ভালো আছি") || lower.contains("খুব খুশি") || lower.contains("দারুণ") ||
            lower.contains("bhalo achi") || lower.contains("valo achi") || lower.contains("khub khushi") ||
            lower.contains("i am fine") || lower.contains("i'm fine") || lower.contains("i am good") ||
            lower.contains("i'm good") || lower.contains("i am happy") || lower.contains("i'm happy") ||
            lower.contains("going great") || lower.contains("feeling better") || lower.contains("ভালো লাগছে")
        if (isHappyOrFine) {
            memory.lastTopic = "positive_day"
            memory.lastQuestionType = AriaQuestionType.ASKED_HOBBY
            val enReply = "That makes me so happy to hear$nameTag! What do you enjoy doing when you have free time — sports, movies, tech, or traveling?"
            val bnReply = "শুনে ভীষণ ভালো লাগল$nameTag! অবসর সময়ে আপনার কী করতে সবচেয়ে ভালো লাগে — খেলাধুলা, মুভি, টেকনোলজি, নাকি ঘুরতে যাওয়া?"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("I really enjoy spending my free time on hobbies.", "আই রিয়েলি এনজয় স্পেন্ডিং মাই ফ্রি টাইম...", if (isBn) "অবসর সময়ে নিজের শখের কাজ করতে আমার খুব ভালো লাগে।" else "Talk about enjoying free time.")
                    ),
                    practicePrompt = if (isBn) "আপনার প্রিয় শখ বা পছন্দের বিষয় কী? সেটাই নিয়ে কথা বলি!" else "What's your favorite hobby or topic?",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("আমার ক্রিকেট ভালো লাগে", "টেকনোলজি ও মোবাইল", "ভ্রমণ করতে ভালোবাসি", "যেকোনো তথ্য জানব")
                    else listOf("I love cricket", "Technology & apps", "I love traveling", "Quiz me")
                )
            )
        }

        // H. Greetings (Assalamu Alaikum, Hello, Hi, Nomoshkar, Salam)
        if (lower.matches(Regex("^(hi|hello|hey|good morning|good afternoon|good evening|assalamu alaikum|salam|hlw|hlo|হ্যালো|হাই|সালাম|আসসালামু আলাইকুম|নমস্কার|শুভ সকাল|শুভ সন্ধ্যা)[!.?\\s]*$"))) {
            val isSalam = lower.contains("salam") || lower.contains("সালাম")
            memory.lastQuestionType = AriaQuestionType.ASKED_DAY
            val enReply = "Hello$nameTag! Great to see you. We can chat about anything, look up facts on any topic, or practice English together. How are you today?"
            val bnReply = if (isSalam) {
                "ওয়ালাইকুম আসসালাম$nameTag! কেমন আছেন? আজ আমরা যেকোনো বিষয়ে কথা বলতে পারি, যেকোনো তথ্য জানতে পারি অথবা ইংরেজি প্র্যাকটিস করতে পারি।"
            } else {
                "হ্যালো$nameTag! কেমন আছেন? আজ আপনি কী নিয়ে কথা বলতে চান, নাকি নতুন কোনো তথ্য বা ইংরেজি শিখবেন?"
            }
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("Hello! How is your day going?", "হ্যালো! হাউ ইজ ইয়োর ডে গোয়িং?", if (isBn) "হ্যালো! আপনার দিন কেমন যাচ্ছে?" else "Friendly natural greeting."),
                        VocabPhrase("I'd love to learn something new today.", "আইড লাভ টু লার্ন সামথিং নিউ টুডে।", if (isBn) "আজ আমি নতুন কিছু শিখতে চাই।" else "Set an upbeat tone.")
                    ),
                    practicePrompt = if (isBn) "বলুন, আজ কীভাবে শুরু করবেন?" else "How would you like to start today?",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("আমি ভালো আছি", "বাংলাদেশ সম্পর্কে বলো", "not sure if you মানে কি", "কুইজ দিন")
                    else listOf("I'm doing great!", "Tell me about AI", "Quiz me")
                )
            )
        }

        // I. Thank you / Goodbye (Bengali, Banglish, English)
        if (lower.contains("ধন্যবাদ") || lower.contains("থ্যাংক") || lower.contains("dhonnobad") ||
            lower.contains("thanks") || lower.contains("thank you") || lower == "tnx" || lower == "ty"
        ) {
            val enReply = "You're very welcome$nameTag! Anytime you want to chat, learn English, or ask any question, I'm right here."
            val bnReply = "আপনাকেও অসংখ্য ধন্যবাদ$nameTag! যখনই কোনো তথ্য লাগবে বা কথা বলতে ইচ্ছে করবে, নির্দ্বিধায় আমাকে বলবেন।"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("You're welcome! Happy to help anytime.", "ইয়োর ওয়েলকাম! হ্যাপি টু হেল্প এনিটাইম।", if (isBn) "স্বাগতম! যেকোনো সময় সাহায্য করতে পেরে খুশি।" else "Warm reply to thanks.")
                    ),
                    practicePrompt = if (isBn) "আর কিছু জানতে চান?" else "Anything else I can help with?",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("আরেকটি তথ্য জানব", "কুইজ দিন", "Past Tense শেখান") else listOf("Ask another fact", "Quiz me")
                )
            )
        }

        if (lower.matches(Regex("^(bye|goodbye|good night|see you|আল্লাহ হাফেজ|বিদায়|আসি|শুভ রাত্রি|allah hafez)[!.?\\s]*$"))) {
            val enReply = "Take care$nameTag! It was wonderful talking with you. See you again soon!"
            val bnReply = "ভালো থাকবেন$nameTag! আপনার সাথে কথা বলে খুব ভালো লাগল। আবার দেখা হবে!"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("Take care and see you soon!", "টেক কেয়ার অ্যান্ড সি ইউ সুন!", if (isBn) "ভালো থাকবেন, শীঘ্রই দেখা হবে!" else "Warm goodbye.")
                    ),
                    practicePrompt = if (isBn) "আবার কথা বলতে চাইলে শুধু 'হ্যালো' বললেই হবে!" else "Say hello anytime you're back!",
                    grammarFeedback = null,
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("হ্যালো!", "কুইজ দিন") else listOf("Hello!", "Quiz me")
                )
            )
        }

        // J. Direct answer to Aria's previous question (Origin / Hobby) so she truly keeps pace!
        if (memory.lastQuestionType == AriaQuestionType.ASKED_ORIGIN && trimmed.split(Regex("\\s+")).size <= 6 && !trimmed.contains("?")) {
            val place = trimmed.replace(Regex("""(?i)^(আমি|i am from|i'm from|from)\s+"""), "")
                .replace(Regex("""(থেকে বলছি|থেকে|থাকি)$"""), "").trim().ifBlank { trimmed }
            memory.userOrigin = place
            memory.lastTopic = place
            memory.lastQuestionType = AriaQuestionType.ASKED_HOBBY
            val enReply = "$place sounds like a wonderful place$nameTag! What do you like most about living there, or what are your favorite hobbies?"
            val bnReply = "বাহ, $place তো দারুণ সুন্দর জায়গা$nameTag! ওখানকার কোন জিনিসটা আপনার সবচেয়ে ভালো লাগে, আর অবসর সময়ে কী করতে ভালোবাসেন?"
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enReply,
                    nativeExplanation = if (isBn) "$bnReply\n\n🇬🇧 $enReply" else enReply,
                    phrases = listOf(
                        VocabPhrase("I live in $place, and people there are very friendly.", "আই লিভ ইন $place...", if (isBn) "নিজের শহর সম্পর্কে ইংরেজিতে সুন্দরভাবে বলতে।" else "Describe where you live warmly.")
                    ),
                    practicePrompt = if (isBn) "আপনার প্রিয় শখ বা $place সম্পর্কে আরও কিছু বলুন, অথবা $place নিয়ে তথ্য জানতে নিচে ট্যাপ করুন!" else "Tell me about your hobbies or ask facts about $place!",
                    grammarFeedback = if (isBn) "✅ বুঝতে পেরেছি আপনি $place থেকে বলছেন!" else "✅ Noted you're from $place!",
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("$place সম্পর্কে বলো", "আমার ক্রিকেট ভালো লাগে", "টেকনোলজি ও মোবাইল")
                    else listOf("Tell me about $place", "I love sports", "I love technology")
                )
            )
        }

        // K. Hobby / Interest follow-up matching (Cricket, Football, Tech, Movies, Travel, Cooking)
        val hobbyMatch = when {
            lower.contains("ক্রিকেট") || lower.contains("cricket") -> Triple("cricket", "ক্রিকেট", "Cricket is so exciting! Who is your favorite player, and do you prefer playing or watching matches?")
            lower.contains("ফুটবল") || lower.contains("football") || lower.contains("soccer") -> Triple("football", "ফুটবল", "Football is thrilling! Which team or player do you support?")
            lower.contains("টেকনোলজি") || lower.contains("মোবাইল") || lower.contains("কোডিং") || lower.contains("tech") || lower.contains("coding") -> Triple("technology", "টেকনোলজি", "Technology moves so fast! Are you interested in mobile apps, AI, or gadgets?")
            lower.contains("ভ্রমণ") || lower.contains("ঘুরতে") || lower.contains("travel") -> Triple("travel", "ভ্রমণ", "Traveling opens up the whole world! Which place is at the top of your dream travel list?")
            lower.contains("রান্না") || lower.contains("খাবার") || lower.contains("cook") || lower.contains("biryani") -> Triple("cooking", "রান্না ও খাবার", "Food brings everyone together! What dish do you love eating or cooking the most?")
            lower.contains("মুভি") || lower.contains("সিনেমা") || lower.contains("গান") || lower.contains("movie") || lower.contains("music") -> Triple("movies & music", "মুভি ও গান", "That's a great way to unwind! What kind of movies or music do you enjoy the most?")
            else -> null
        }
        if (hobbyMatch != null && !looksLikeExplicitFactQuestion(trimmed)) {
            val (topicEn, topicBn, enFollowUp) = hobbyMatch
            memory.lastTopic = topicEn
            memory.lastQuestionType = AriaQuestionType.NONE
            val bnFollowUp = when (topicEn) {
                "cricket" -> "ক্রিকেট দারুণ উত্তেজনাপূর্ণ খেলা$nameTag! আপনার প্রিয় খেলোয়াড় কে, আর আপনি কি নিজে খেলতে বেশি ভালোবাসেন নাকি খেলা দেখতে?"
                "football" -> "ফুটবল আমারও খুব পছন্দের টপিক$nameTag! আপনি কোন দল বা খেলোয়াড়ের ভক্ত?"
                "technology" -> "টেকনোলজি নিয়ে আগ্রহ থাকা দারুণ$nameTag! আপনার কি মোবাইল অ্যাপ, এআই (AI), নাকি নতুন গ্যাজেট বেশি ভালো লাগে?"
                "travel" -> "ঘুরতে যাওয়া সত্যিই দারুণ অভিজ্ঞতা$nameTag! আপনি সামনে কোন জায়গায় ঘুরতে যেতে চান?"
                "cooking" -> "মজার খাবার সবাই ভালোবাসে$nameTag! আপনার সবচেয়ে প্রিয় খাবার কোনটি?"
                else -> "অবসর কাটানোর দারুণ উপায়$nameTag! আপনার কোন ধরনের মুভি বা গান সবচেয়ে বেশি ভালো লাগে?"
            }
            return CompanionAction.DirectResponse(
                TutorResponse(
                    spokenText = enFollowUp,
                    nativeExplanation = if (isBn) "$bnFollowUp\n\n🇬🇧 $enFollowUp" else enFollowUp,
                    phrases = listOf(
                        VocabPhrase("I'm a big fan of $topicEn.", "আইম আ বিগ ফ্যান অব $topicEn।", if (isBn) "আমি $topicBn-এর খুব বড় ভক্ত।" else "Express enthusiasm for your interest."),
                        VocabPhrase("It helps me relax after a busy day.", "ইট হেল্পস মি রিল্যাক্স আফটার আ বিজি ডে।", if (isBn) "ব্যস্ত দিনের পর এটি আমাকে মন ভালো করতে সাহায্য করে।" else "Explain why you enjoy it.")
                    ),
                    practicePrompt = if (isBn) "উপরের প্রশ্নের উত্তর দিন, অথবা $topicBn নিয়ে যেকোনো তথ্য জানতে চাইতে পারেন!" else "Reply to keep our chat flowing, or ask any fact about $topicEn!",
                    grammarFeedback = if (isBn) "✅ আপনার পছন্দ ($topicBn) বুঝে উত্তর দিয়েছি!" else "✅ Great topic: $topicEn!",
                    isSampleResponse = true,
                    quickReplies = if (isBn) listOf("$topicBn সম্পর্কে বলো", "আমি ইংরেজিতে কথা বলতে চাই", "কুইজ দিন")
                    else listOf("Tell me about $topicEn", "Quiz me")
                )
            )
        }

        return CompanionAction.NotHandled
    }

    // -------------------------------------------------------------------------
    // 3. FACTUAL / GENERAL KNOWLEDGE QUERY EXTRACTOR
    // -------------------------------------------------------------------------

    fun extractKnowledgeQuery(raw: String, module: LearningModule): String? {
        val trimmed = raw.trim()
        if (trimmed.length < 2) return null

        // If inside "Ask Anything" (যেকোনো তথ্য) mode, treat almost any query as a live info lookup!
        if (module == LearningModule.ASK_INFO) {
            return cleanKnowledgeSubject(trimmed).ifBlank { trimmed }
        }

        // In other modes (like Conversation), trigger live knowledge lookup whenever the user asks a factual question
        if (looksLikeExplicitFactQuestion(trimmed)) {
            val subject = cleanKnowledgeSubject(trimmed)
            if (subject.length >= 2) return subject
        }

        return null
    }

    fun looksLikeExplicitFactQuestion(raw: String): Boolean {
        val lower = raw.lowercase().trim()
        val bnFactMarkers = listOf(
            "সম্পর্কে বলো", "সম্পর্কে বলুন", "সম্পর্কে জানাও", "সম্পর্কে জানতে", "সম্বন্ধে",
            "কাকে বলে", "কে ছিলেন", "কে আবিষ্কার", "আবিষ্কারক কে", "জনক কে", "রাজধানী",
            "কোথায় অবস্থিত", "কোন দেশে", "কবে স্বাধীন", "কবে জন্ম", "ইতিহাস বলো", "ইতিহাস কী",
            "তথ্য দাও", "তথ্য বলো", "জানতে চাই", "ব্যাখ্যা করো", "কীভাবে কাজ করে", "কিভাবে কাজ করে",
            "কতটি জেলা", "সবচেয়ে বড়", "সবচেয়ে ছোট", "বৃহত্তম", "ক্ষুদ্রতম", "মহাকাশ", "সৌরজগৎ",
            "কৃত্রিম বুদ্ধিমত্তা", "somporke bolo", "kake bole", "ke chilen", "rajdhani"
        )
        if (bnFactMarkers.any { lower.contains(it) }) return true

        val enFactPatterns = listOf(
            Regex("""^(who is|who was|who invented|who discovered|who founded)\s+.+"""),
            Regex("""^(what is|what are|what was)\s+(?!your name|the time|the meaning).+"""),
            Regex("""^(where is|when was|when did|how does|why does|why is)\s+.+"""),
            Regex("""^(tell me about|info about|information about|facts? about|history of|capital of|explain)\s+.+""")
        )
        return enFactPatterns.any { it.containsMatchIn(lower) }
    }

    private fun cleanKnowledgeSubject(raw: String): String {
        var s = raw.trim().trim('?', '!', '।', '.', '"', '\'')
        // Strip Bengali wrapper phrases
        val bnPrefixes = listOf(
            "আমাকে বলো ", "আমাকে বলুন ", "আমাকে জানাও ", "বলো তো ", "বলুন তো ",
            "আমি জানতে চাই ", "জানতে চাই ", "তথ্য দাও "
        )
        bnPrefixes.forEach { p -> if (s.startsWith(p)) s = s.removePrefix(p).trim() }

        s = s.replace(
            Regex("""(?i)\s*(?:এর\s*)?(?:সম্পর্কে বলো|সম্পর্কে বলুন|সম্পর্কে জানাও|সম্পর্কে জানতে চাই|সম্পর্কে তথ্য দাও|সম্পর্কে|সম্বন্ধে বলো|সম্বন্ধে|কাকে বলে|কে ছিলেন|কে আবিষ্কার করেছেন|কে আবিষ্কার করে|কোথায় অবস্থিত|তথ্য দাও|তথ্য বলো|ব্যাখ্যা করো|ব্যাখ্যা করুন|জানতে চাই|কী\?|কি\?|somporke bolo|kake bole|ke chilen)$"""),
            ""
        ).trim()

        // Strip English question wrappers
        s = s.replace(
            Regex("""(?i)^(?:tell me about|can you tell me about|give me information about|info about|information about|facts? about|who is|who was|what is|what are|what was|where is|explain|history of)\s+(?:a |an |the )?"""),
            ""
        ).trim()

        return s.trim('?', '!', '।', '.', ',', '-', ':')
    }

    private fun normalizeBengaliDigits(input: String): String {
        val bn = "০১২৩৪৫৬৭৮৯"
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = bn.indexOf(ch)
            sb.append(if (idx >= 0) ('0' + idx) else ch)
        }
        return sb.toString()
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
