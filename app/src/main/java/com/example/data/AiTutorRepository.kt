package com.example.data

import com.example.model.InspireSuggestion
import com.example.model.LearningModule
import com.example.model.RolePlayScenario
import com.example.model.TutorMode
import com.example.model.TutorResponse
import com.example.model.UserSettings
import com.example.model.VocabPhrase
import kotlinx.coroutines.delay

/**
 * Unified AI tutor interface. Backed by [PlaceholderAiTutorRepository] (combining live keyless
 * Wikipedia/DuckDuckGo knowledge search, live multilingual translation & English dictionary lookup,
 * multi-turn conversational memory, Banglish/Bengali/English intent understanding, grammar checking,
 * and interactive quizzes) when Firebase AI Logic is not enabled.
 */
interface AiTutorRepository {
    val isPlaceholderMode: Boolean

    suspend fun generateLessonIntro(
        settings: UserSettings,
        mode: TutorMode,
        topic: String,
        module: LearningModule = LearningModule.CONVERSATION
    ): TutorResponse

    suspend fun generateRolePlayIntro(
        settings: UserSettings,
        scenario: RolePlayScenario
    ): TutorResponse

    suspend fun sendMessage(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        userText: String,
        turnCount: Int,
        module: LearningModule = LearningModule.CONVERSATION
    ): TutorResponse

    suspend fun generateInspireHints(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        lastAiMessage: String?,
        module: LearningModule = LearningModule.CONVERSATION
    ): List<InspireSuggestion>
}

/**
 * Smart Interactive Tutor & Knowledge Companion used when Firebase AI Logic is not provisioned.
 * Remains flagged as [isPlaceholderMode] = true so the UI transparently notes that full generative
 * Gemini AI requires Firebase AI Logic.
 */
class PlaceholderAiTutorRepository(
    private val lookupService: LiveLanguageLookupService = LiveLanguageLookupService()
) : AiTutorRepository {

    override val isPlaceholderMode: Boolean = true

    private val memory = ConversationMemory()
    private var activeQuizQuestion: QuizQuestion? = null
    private var quizCursor: Int = 0
    private var lastLookedUpPhrase: String? = null

    override suspend fun generateLessonIntro(
        settings: UserSettings,
        mode: TutorMode,
        topic: String,
        module: LearningModule
    ): TutorResponse {
        delay(180)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val level = settings.level
        val isBn = native.code == "bn"

        if (module == LearningModule.QUIZ) {
            return startNextQuizQuestion(settings, feedbackPrefix = null)
        }

        val phrases = TutorCurriculumBank.modulePhrases(module, target.code, native.code, level, topic)

        val openerNative = when (module) {
            LearningModule.CONVERSATION -> if (isBn) {
                "নমস্কার! আমি আরিয়া। আপনি আমার সাথে বাংলা, বাংলিশ বা ইংরেজিতে মন খুলে কথা বলুন — আমি আপনার কথার সাথে তাল মিলিয়ে উত্তর দেব, গ্রামার ঠিক করে দেব এবং যেকোনো তথ্য চাইলে সাথে সাথে খুঁজে দেব!"
            } else {
                "Hello! I'm Aria. Chat with me naturally, ask any real-world fact, or practice ${level.label} (${level.cefr}) ${target.englishName}!"
            }

            LearningModule.ASK_INFO -> if (isBn) {
                "🌐 যেকোনো তথ্য মোড চালু হয়েছে! দেশ, ইতিহাস, বিজ্ঞান, টেকনোলজি, বিখ্যাত ব্যক্তি, বর্তমান সময়/তারিখ বা গণিতের হিসাব — যা জানতে চান বাংলা বা ইংরেজিতে লিখুন অথবা মাইকে বলুন, আমি সাথে সাথে সঠিক তথ্য জানিয়ে দেব।"
            } else {
                "🌐 Ask Anything Mode is ready! Ask about history, science, countries, famous people, math, unit conversions, or live time/date — I'll fetch clear answers in ${native.nativeName} and ${target.englishName}."
            }

            LearningModule.TRANSLATE -> if (isBn) {
                "🔍 অর্থ ও অনুবাদ মোড! যেকোনো ইংরেজি শব্দ বা বাক্য (যেমন: \"not sure if you মানে কি\") অথবা বাংলা বাক্য লিখুন — আমি অনলাইন ডিকশনারি ও অনুবাদ থেকে সঠিক অর্থ, উচ্চারণ ও উদাহরণ জানিয়ে দেব।"
            } else {
                "🔍 Meaning & Translate Mode! Type any word or sentence for live translation, IPA phonetics, and examples."
            }

            LearningModule.GRAMMAR -> {
                val first = TutorCurriculumBank.grammarLessons.first()
                if (isBn) "📖 ${first.titleBn}: ${first.summaryBn}" else "📖 ${first.titleEn}: ${first.summaryEn}"
            }

            LearningModule.VOCABULARY -> if (isBn) {
                "🔤 আজকের ${level.bnLabel} (${level.cefr}) শব্দভান্ডার! যেকোনো নতুন ইংরেজি শব্দ টাইপ করলে আমি তার বাংলা অর্থ, উচ্চারণ (IPA) ও সমার্থক শব্দ বলে দেব।"
            } else {
                "🔤 Today's ${level.label} (${level.cefr}) Vocabulary! Type any word for live definition, phonetics, and synonyms."
            }

            LearningModule.PRONUNCIATION -> if (isBn) {
                "🗣️ উচ্চারণ অনুশীলন! প্রতিটি কার্ডে ট্যাপ করে মধুর কণ্ঠে শুনুন এবং নিজে বলার চেষ্টা করুন:"
            } else {
                "🗣️ Pronunciation Studio! Tap any card to hear natural stress and rhythm, then repeat aloud."
            }

            LearningModule.OFFICE -> if (isBn) {
                "💼 অফিস, মিটিং ও জব ইন্টারভিউয়ের স্মার্ট ইংরেজি! নিচের বাক্যগুলো শিখুন বা নিজের কাজের কথা লিখে প্র্যাকটিস করুন:"
            } else {
                "💼 Workplace & Interview English! Practice introducing your skills or phrasing professional updates."
            }

            LearningModule.TRAVEL -> if (isBn) {
                "✈️ এয়ারপোর্ট, হোটেল ও রেস্তোরাঁর দরকারি ইংরেজি বাক্য! শুনুন ও অনুশীলন করুন:"
            } else {
                "✈️ Essential Travel & Dining expressions! Practice ordering, checking in, or asking directions."
            }

            LearningModule.QUIZ -> ""
        }

        val quickReplies = when (module) {
            LearningModule.CONVERSATION -> if (isBn) listOf(
                "তুমি কেমন আছো?",
                "বাংলাদেশ সম্পর্কে বলো",
                "not sure if you মানে কি?",
                "কুইজ দিন"
            ) else listOf("How are you?", "Tell me about Space", "What does 'curious' mean?", "Quiz me")

            LearningModule.ASK_INFO -> if (isBn) listOf(
                "বাংলাদেশ সম্পর্কে বলো",
                "কৃত্রিম বুদ্ধিমত্তা কী?",
                "আলবার্ট আইনস্টাইন কে ছিলেন?",
                "এখন কয়টা বাজে?",
                "25 * 48 কত?"
            ) else listOf("Tell me about Bangladesh", "What is Artificial Intelligence?", "Who was Albert Einstein?", "What time is it?", "25 * 48")

            LearningModule.TRANSLATE -> if (isBn) listOf(
                "not sure if you মানে কি",
                "take it easy মানে কি",
                "আমি ইংরেজি শিখতে চাই",
                "resilient meaning"
            ) else listOf("What does 'not sure if you' mean?", "Meaning of resilient", "How to say 'I am grateful'")

            LearningModule.GRAMMAR -> if (isBn) TutorCurriculumBank.grammarLessons.first().followUpRepliesBn
            else TutorCurriculumBank.grammarLessons.first().followUpRepliesEn

            LearningModule.VOCABULARY -> if (isBn) listOf("serendipity meaning", "consistent মানে কি", "কুইজ দিন")
            else listOf("Meaning of serendipity", "Meaning of consistent", "Quiz me")

            LearningModule.PRONUNCIATION -> if (isBn) listOf("entrepreneur meaning", "schedule মানে কি", "কুইজ দিন")
            else listOf("Meaning of entrepreneur", "Quiz me")

            LearningModule.OFFICE -> if (isBn) listOf("I work as a software developer", "ফলো-আপ ইমেইল ইংরেজি কি", "Polite Modals (Could/Would)")
            else listOf("Tell me about your strengths", "Polite Modals", "Quiz me")

            LearningModule.TRAVEL -> if (isBn) listOf("বিলটি দেবেন ইংরেজি কি", "I'd like a window seat", "কুইজ দিন")
            else listOf("Could I get a window seat?", "Where is the nearest metro?")

            LearningModule.QUIZ -> emptyList()
        }

        val followUp = if (isBn) {
            when (module) {
                LearningModule.ASK_INFO -> "নিচের যেকোনো প্রশ্নে ট্যাপ করুন, অথবা আপনার মনের যেকোনো প্রশ্ন লিখে বা বলে জিজ্ঞেস করুন!"
                LearningModule.TRANSLATE -> "যেকোনো ইংরেজি বা বাংলা শব্দ/বাক্য লিখে পাঠান — আমি এখনই সঠিক অর্থ খুঁজে দেব!"
                LearningModule.GRAMMAR -> "নিচের যেকোনো গ্রামার টপিকে ট্যাপ করুন, অথবা একটি ইংরেজি বাক্য লিখুন — আমি ভুল থাকলে ঠিক করে দেব!"
                else -> "নিজের মতো করে যেকোনো কথা বলুন বা প্রশ্ন করুন — আমি আপনার কথার সাথে তাল মিলিয়ে উত্তর দেব!"
            }
        } else {
            "Tap a quick suggestion below, or speak/type anything you want to ask or discuss!"
        }

        return TutorResponse(
            spokenText = if (isBn) "নমস্কার! চলুন শুরু করি।" else "Hello! Let's get started!",
            nativeExplanation = if (settings.studyInNativeLanguage) openerNative else "Let's explore ${module.label} (${level.label}).",
            phrases = phrases,
            practicePrompt = followUp,
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = quickReplies
        )
    }

    override suspend fun generateRolePlayIntro(
        settings: UserSettings,
        scenario: RolePlayScenario
    ): TutorResponse {
        delay(200)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val spokenText = TutorCurriculumBank.rolePlayOpeningLine(target.code, scenario.id)
        val phrases = TutorCurriculumBank.rolePlayStarterPhrases(target.code, native.code, scenario.id)

        val nativeExplanation = if (settings.studyInNativeLanguage && isBn) {
            "রোল-প্লে শুরু হলো! (${scenario.title}) আমি এখন আপনার ${scenario.aiRoleTitle}। আমি বললাম: \"$spokenText\""
        } else {
            spokenText
        }

        return TutorResponse(
            spokenText = spokenText,
            nativeExplanation = nativeExplanation,
            phrases = phrases,
            practicePrompt = if (isBn) "নিচের যেকোনো বাক্যে ট্যাপ করুন অথবা Speak/Type দিয়ে নিজের উত্তর বলুন!" else "Tap a suggested reply or respond in character!",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = phrases.map { it.targetText }
        )
    }

    override suspend fun sendMessage(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        userText: String,
        turnCount: Int,
        module: LearningModule
    ): TutorResponse {
        val trimmed = userText.trim()
        val lower = trimmed.lowercase()

        // 1. Active Quiz grading
        val pendingQuiz = activeQuizQuestion
        if (pendingQuiz != null && (module == LearningModule.QUIZ || looksLikeQuizAnswer(trimmed, pendingQuiz))) {
            return evaluateQuizAnswer(settings, pendingQuiz, trimmed)
        }

        // 2. Explicit Quiz trigger from anywhere
        if (lower.contains("কুইজ") || lower == "quiz" || lower.contains("quiz me") || lower.contains("test me") || lower.contains("পরীক্ষা")) {
            return startNextQuizQuestion(settings, feedbackPrefix = null)
        }

        // 3. Instant real-time utilities (Current Time, Date, Day, Math, Unit Conversion)
        ConversationalEngine.tryAnswerUtility(trimmed, settings)?.let { return it }

        // 4. Example sentences for last looked-up word/phrase
        if ((lower.contains("বাক্যে ব্যবহার") || lower.contains("উদাহরণ") || lower.contains("example sentence") || lower.contains("use in a sentence")) &&
            !lastLookedUpPhrase.isNullOrBlank()
        ) {
            return buildUsageExamplesResponse(settings, lastLookedUpPhrase!!)
        }

        // 5. Grammar / Tense request
        detectGrammarTopic(lower, module)?.let { return buildGrammarLessonResponse(settings, it) }

        // 6. Role-Play scenario mode
        if (scenario != null) {
            return handleRolePlayReply(settings, scenario, trimmed)
        }

        // 7. Meaning / Translation lookup (e.g., "not sure if you মানে কি", "serendipity meaning", or Translate module)
        extractMeaningQuery(trimmed, module)?.let { query ->
            return handleLiveMeaningOrTranslation(settings, rawUserText = trimmed, query = query)
        }

        // 8. Multi-turn context & empathetic companion (Banglish, Bengali & English conversation)
        when (val companionAction = ConversationalEngine.tryHandleCompanionOrContext(trimmed, settings, memory)) {
            is CompanionAction.DirectResponse -> return companionAction.response
            is CompanionAction.LookupKnowledge -> {
                return handleLiveKnowledgeLookup(settings, companionAction.query, isFollowUpMore = companionAction.isFollowUpMore)
            }
            CompanionAction.NotHandled -> Unit
        }

        // 9. Live General Knowledge & Information lookup (Ask Anything module OR any factual question anywhere!)
        ConversationalEngine.extractKnowledgeQuery(trimmed, module)?.let { knowledgeQuery ->
            return handleLiveKnowledgeLookup(settings, knowledgeQuery, isFollowUpMore = false)
        }

        // 10. User wrote a custom Bengali/native sentence -> understand, reply warmly, AND show natural English!
        if (isPredominantlyNativeScript(trimmed, settings.nativeLanguageCode, settings.targetLanguageCode)) {
            return handleNativeSentenceConversation(settings, trimmed)
        }

        // 11. Custom English/Target sentence -> grammar check + live translation + contextual reply!
        return handleTargetConversationReply(settings, trimmed, turnCount)
    }

    override suspend fun generateInspireHints(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        lastAiMessage: String?,
        module: LearningModule
    ): List<InspireSuggestion> {
        delay(100)
        if (scenario != null) {
            return TutorCurriculumBank.rolePlayStarterPhrases(settings.targetLanguageCode, settings.nativeLanguageCode, scenario.id).map {
                InspireSuggestion(it.targetText, it.nativeMeaning)
            }
        }
        return TutorCurriculumBank.modulePhrases(module, settings.targetLanguageCode, settings.nativeLanguageCode, settings.level, "Daily Life")
            .take(3)
            .map { InspireSuggestion(it.targetText, it.nativeMeaning) }
    }

    // -------------------------------------------------------------------------
    // LIVE GENERAL KNOWLEDGE & INFORMATION LOOKUP (WIKIPEDIA + DUCKDUCKGO)
    // -------------------------------------------------------------------------

    private suspend fun handleLiveKnowledgeLookup(
        settings: UserSettings,
        query: String,
        isFollowUpMore: Boolean
    ): TutorResponse {
        val native = settings.nativeLanguage
        val target = settings.targetLanguage
        val isBn = native.code == "bn"
        val queryIsNative = isPredominantlyNativeScript(query, native.code, target.code)
        val preferredLang = if (queryIsNative || isBn) native.code else target.code

        val result = lookupService.searchKnowledge(query, primaryLangCode = preferredLang)
        if (result != null) {
            memory.lastTopic = result.title
            memory.lastKnowledgeTitle = result.title
            memory.lastRelatedTopics = result.relatedTitles
            memory.lastQuestionType = AriaQuestionType.ASKED_KNOWLEDGE_MORE
            lastLookedUpPhrase = result.title

            val firstSentence = result.summary.split(Regex("(?<=[.।!?])\\s+")).firstOrNull()?.take(180) ?: result.summary.take(180)

            // Provide bilingual summary: if Wikipedia returned English and user prefers Bengali, translate it live!
            val nativeSummary: String
            val targetHighlight: String
            if (result.langCode == native.code) {
                nativeSummary = result.summary
                targetHighlight = lookupService.translate(firstSentence, native.code, target.code)?.translatedText
                    ?: result.title
            } else {
                targetHighlight = firstSentence
                nativeSummary = if (settings.studyInNativeLanguage) {
                    lookupService.translate(result.summary, result.langCode, native.code)?.translatedText
                        ?: result.summary
                } else {
                    result.summary
                }
            }
            memory.lastSimpleSummary = nativeSummary.take(220)

            val introPrefix = if (isFollowUpMore) {
                if (isBn) "🌐 আরও তথ্য — **${result.title}**:\n\n" else "🌐 More about ${result.title}:\n\n"
            } else {
                val descPart = result.description.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                "🌐 ${result.title}$descPart\n\n"
            }

            val phrases = mutableListOf<VocabPhrase>()
            if (targetHighlight.isNotBlank()) {
                phrases.add(
                    VocabPhrase(
                        targetText = targetHighlight,
                        pronunciation = if (isBn) "ইংরেজিতে মূল তথ্য (ট্যাপ করে শুনুন)" else "Key takeaway in ${target.englishName}",
                        nativeMeaning = if (isBn) firstSentence else result.description.ifBlank { result.title }
                    )
                )
            }

            val followReplies = buildList {
                if (result.relatedTitles.isNotEmpty()) {
                    add(if (isBn) "আরও বলো" else "Tell me more")
                    result.relatedTitles.take(2).forEach { rel ->
                        add(if (isBn) "$rel সম্পর্কে বলো" else "Tell me about $rel")
                    }
                }
                add(if (isBn) "সহজ করে বলো" else "Explain simply")
            }.distinct().take(4)

            return TutorResponse(
                spokenText = if (isBn) nativeSummary else targetHighlight,
                nativeExplanation = "$introPrefix$nativeSummary",
                phrases = phrases,
                practicePrompt = if (isBn) {
                    "এই বিষয়ে আরও জানতে 'আরও বলো' ট্যাপ করুন, অথবা নতুন যেকোনো প্রশ্ন করুন!"
                } else {
                    "Tap 'Tell me more' to dive deeper, or ask any other question!"
                },
                grammarFeedback = if (isBn) "✅ লাইভ তথ্যসূত্র: ${result.sourceLabel}" else "✅ Live info from ${result.sourceLabel}",
                isSampleResponse = true,
                quickReplies = followReplies
            )
        }

        // Honest, helpful fallback if Wikipedia/DuckDuckGo couldn't find an exact match or device is offline
        val fallbackBn = "দুঃখিত, «$query» নিয়ে অনলাইন তথ্যভান্ডারে সরাসরি নিবন্ধ পাওয়া যায়নি অথবা ইন্টারনেট সংযোগ ধীর। বিষয়ের মূল নামটি একটু স্পষ্ট করে লিখলে (যেমন: 'বাংলাদেশ', 'সৌরজগৎ', 'পাইথন প্রোগ্রামিং') আমি সাথে সাথে তথ্য বের করে দিতে পারব!"
        val fallbackEn = "I couldn't find a direct encyclopedia match for \"$query\" right now. Try entering the core topic name (e.g., 'Bangladesh', 'Solar System', 'Albert Einstein') or check your internet connection."
        return TutorResponse(
            spokenText = fallbackEn,
            nativeExplanation = if (isBn) fallbackBn else fallbackEn,
            phrases = emptyList(),
            practicePrompt = if (isBn) "নিচের উদাহরণগুলোতে ট্যাপ করে চেষ্টা করে দেখতে পারেন:" else "Try one of these topics below:",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("বাংলাদেশ সম্পর্কে বলো", "সৌরজগৎ সম্পর্কে বলো", "কৃত্রিম বুদ্ধিমত্তা কী?", "এখন কয়টা বাজে?")
            else listOf("Tell me about Earth", "What is AI?", "What time is it?")
        )
    }

    // -------------------------------------------------------------------------
    // LIVE TRANSLATION & DICTIONARY LOOKUP
    // -------------------------------------------------------------------------

    private suspend fun handleLiveMeaningOrTranslation(
        settings: UserSettings,
        rawUserText: String,
        query: String
    ): TutorResponse {
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val cleanQuery = query.trim().trim('"', '\'', '?', '!', '.', '।')
        lastLookedUpPhrase = cleanQuery
        memory.lastTopic = cleanQuery

        val queryIsNativeScript = isPredominantlyNativeScript(cleanQuery, native.code, target.code)
        val explicitToTarget = rawUserText.contains("ইংরেজি") || rawUserText.lowercase().contains("in english") ||
            rawUserText.lowercase().contains("how to say") || rawUserText.lowercase().contains("to ${target.englishName.lowercase()}")

        val fromCode = if (queryIsNativeScript || explicitToTarget) native.code else target.code
        val toCode = if (fromCode == native.code) target.code else native.code

        val translation = lookupService.translate(cleanQuery, fromCode, toCode)
        val englishTextForDict = when {
            fromCode == "en" -> cleanQuery
            toCode == "en" && translation != null -> translation.translatedText
            else -> null
        }
        val dictEntry = englishTextForDict?.let { pickDictionaryKeyword(it) }?.let { kw ->
            lookupService.lookupEnglishWord(kw)
        }

        val phrases = mutableListOf<VocabPhrase>()
        if (translation != null) {
            val primaryTarget = if (fromCode == target.code) cleanQuery else translation.translatedText
            val primaryNative = if (fromCode == target.code) translation.translatedText else cleanQuery
            val phoneticLabel = dictEntry?.phonetic?.takeIf { it.isNotBlank() }?.let { ph ->
                if (dictEntry.partOfSpeech.isNotBlank()) "${dictEntry.partOfSpeech} · $ph" else ph
            }.orEmpty()

            phrases.add(VocabPhrase(primaryTarget, phoneticLabel, primaryNative))
            buildCommonExpressionTip(cleanQuery.lowercase(), isBn)?.let { phrases.add(it) }

            if (dictEntry?.example != null) {
                val exTranslated = lookupService.translate(dictEntry.example, "en", native.code)?.translatedText
                phrases.add(
                    VocabPhrase(
                        targetText = dictEntry.example,
                        pronunciation = if (isBn) "বাস্তব উদাহরণ (Dictionary)" else "Dictionary Example",
                        nativeMeaning = exTranslated ?: dictEntry.definition
                    )
                )
            } else if (translation.alternatives.isNotEmpty()) {
                val alt = translation.alternatives.first()
                phrases.add(
                    VocabPhrase(
                        targetText = if (fromCode == target.code) cleanQuery else alt,
                        pronunciation = if (isBn) "বিকল্প অর্থ / প্রকাশ" else "Alternative phrasing",
                        nativeMeaning = if (fromCode == target.code) alt else cleanQuery
                    )
                )
            }

            if (dictEntry != null && dictEntry.synonyms.isNotEmpty()) {
                phrases.add(
                    VocabPhrase(
                        targetText = dictEntry.synonyms.joinToString(", "),
                        pronunciation = if (isBn) "সমার্থক শব্দ (Synonyms)" else "Synonyms",
                        nativeMeaning = if (isBn) "'${dictEntry.word}'-এর কাছাকাছি অর্থের ইংরেজি শব্দ।" else "Similar words to '${dictEntry.word}'."
                    )
                )
            }

            val header = if (isBn) {
                if (fromCode == target.code) {
                    "«$cleanQuery» এর বাংলা অর্থ হলো: «${translation.translatedText}»।" +
                        if (dictEntry != null) "\n\n📖 ডিকশনারি সংজ্ঞা (${dictEntry.word}): ${dictEntry.definition}" else ""
                } else {
                    "«$cleanQuery» — এর ${target.nativeName} (${target.englishName}) হলো: «${translation.translatedText}»।"
                }
            } else {
                "\"$cleanQuery\" translates to: \"${translation.translatedText}\"." +
                    if (dictEntry != null) "\n\n📖 Definition (${dictEntry.word}): ${dictEntry.definition}" else ""
            }
            memory.lastSimpleSummary = "«$cleanQuery» মানে «${translation.translatedText}»"

            return TutorResponse(
                spokenText = if (fromCode == target.code) cleanQuery else translation.translatedText,
                nativeExplanation = header,
                phrases = phrases,
                practicePrompt = if (isBn) "এই শব্দ/বাক্যটি বাস্তব উদাহরণে দেখতে চান, নাকি অন্য কিছু জানতে চান?" else "Want example sentences, or another word/topic?",
                grammarFeedback = if (isBn) "✅ অনলাইন অনুবাদ ও ডিকশনারি থেকে লাইভ উত্তর।" else "✅ Live translation & dictionary lookup.",
                isSampleResponse = true,
                quickReplies = if (isBn) listOf("বাক্যে ব্যবহার দেখান", "কুইজ দিন", "Past Tense শেখান")
                else listOf("Use in a sentence", "Quiz me", "Teach Past Tense")
            )
        }

        val offlineMsg = if (isBn) {
            "দুঃখিত, এই মুহূর্তে অনলাইন অনুবাদে সংযোগ পাওয়া যায়নি («$cleanQuery»)। ইন্টারনেট চালু আছে কিনা দেখে আবার চেষ্টা করুন।"
        } else {
            "Sorry, I couldn't reach the online translation service for \"$cleanQuery\". Please check your internet connection."
        }
        return TutorResponse(
            spokenText = cleanQuery,
            nativeExplanation = offlineMsg,
            phrases = emptyList(),
            practicePrompt = if (isBn) "আবার চেষ্টা করুন অথবা নিচের অপশনগুলো দেখুন।" else "Try again or pick an option below.",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) listOf(rawUserText, "গ্রামার ও টেন্স", "কুইজ দিন") else listOf(rawUserText, "Quiz me")
        )
    }

    private suspend fun buildUsageExamplesResponse(settings: UserSettings, phrase: String): TutorResponse {
        val isBn = settings.nativeLanguageCode == "bn"
        val clean = phrase.trim()
        val lower = clean.lowercase()

        val examples: List<String> = when {
            lower.startsWith("not sure if you") -> listOf(
                "Not sure if you heard, but the meeting was moved to 3 PM.",
                "Not sure if you're free tonight, would you like to grab coffee?",
                "Not sure if you received my email earlier today."
            )
            clean.split(" ").size == 1 -> {
                val dict = lookupService.lookupEnglishWord(clean)
                listOfNotNull(
                    dict?.example,
                    "I am learning how to use '$clean' in daily conversation.",
                    "Could you explain '$clean' with another example?"
                ).distinct().take(3)
            }
            else -> listOf(
                "$clean — could you tell me what you think?",
                "People often say \"$clean\" in everyday conversation.",
                "Let's practice saying \"$clean\" clearly and confidently."
            )
        }

        val vocabList = examples.map { ex ->
            val bn = lookupService.translate(ex, "en", settings.nativeLanguageCode)?.translatedText
                ?: if (isBn) "বাস্তব কথোপকথনে ব্যবহারের উদাহরণ।" else "Natural everyday example."
            VocabPhrase(ex, "", bn)
        }

        return TutorResponse(
            spokenText = examples.first(),
            nativeExplanation = if (isBn) "নিচে «$clean» ব্যবহার করে ৩টি বাস্তব ইংরেজি বাক্য ও বাংলা অর্থ দেওয়া হলো:" else "Here are 3 real-life sentences using \"$clean\":",
            phrases = vocabList,
            practicePrompt = if (isBn) "এবার আপনি নিজে «$clean» দিয়ে একটি ছোট বাক্য লিখে বা বলে চেষ্টা করুন!" else "Now try making your own sentence with \"$clean\"!",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("সহজ করে বলো", "কুইজ দিন", "Past Tense শেখান") else listOf("Quiz me", "Teach Past Tense")
        )
    }

    // -------------------------------------------------------------------------
    // NATURAL BENGALI CONVERSATION + LIVE TARGET TRANSLATION
    // -------------------------------------------------------------------------

    private suspend fun handleNativeSentenceConversation(
        settings: UserSettings,
        userText: String
    ): TutorResponse {
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val nameTag = memory.userName?.let { ", $it" }.orEmpty()

        // If it ends with a question mark, try live knowledge lookup first so we actually answer questions!
        if (userText.endsWith("?") || userText.contains("কী") || userText.contains("কেন") || userText.contains("কোথায়")) {
            val cleaned = userText.trim('?', '!', '।')
            val knowledge = lookupService.searchKnowledge(cleaned, primaryLangCode = native.code)
            if (knowledge != null) {
                return handleLiveKnowledgeLookup(settings, cleaned, isFollowUpMore = false)
            }
        }

        val translated = lookupService.translate(userText, native.code, target.code)
        val targetSentence = translated?.translatedText

        if (targetSentence != null) {
            lastLookedUpPhrase = targetSentence
            memory.lastTopic = userText.take(40)
            memory.lastSimpleSummary = "«$userText» এর ইংরেজি হলো «$targetSentence»"

            val smartReplyEn = buildDynamicConversationalReply(targetSentence.lowercase(), 1)
            val smartReplyBn = lookupService.translate(smartReplyEn, "en", native.code)?.translatedText
                ?: "খুব সুন্দর কথা বলেছেন$nameTag! এই বিষয়ে আরও কিছু বলুন।"

            val phrases = listOf(
                VocabPhrase(
                    targetText = targetSentence,
                    pronunciation = if (isBn) "আপনার কথাটির স্বাভাবিক ${target.englishName}" else "Your thought in ${target.englishName}",
                    nativeMeaning = userText
                ),
                VocabPhrase(
                    targetText = smartReplyEn,
                    pronunciation = if (isBn) "আমার জবাব (ইংরেজিতে)" else "My reply",
                    nativeMeaning = smartReplyBn
                )
            )

            return TutorResponse(
                spokenText = targetSentence,
                nativeExplanation = if (isBn) {
                    "$smartReplyBn\n\nআপনার কথাটি ইংরেজিতে সুন্দরভাবে এভাবে বলতে পারেন: «$targetSentence»।"
                } else {
                    "$smartReplyEn\n\nIn ${target.englishName}: \"$targetSentence\""
                },
                phrases = phrases,
                practicePrompt = if (isBn) "এভাবেই মন খুলে কথা চালিয়ে যান — অথবা যেকোনো তথ্য জানতে জিজ্ঞেস করুন!" else "Keep chatting naturally, or ask any question!",
                grammarFeedback = if (isBn) "✅ আপনার কথাটি বুঝে উত্তর ও ইংরেজি অনুবাদ দেওয়া হয়েছে।" else "✅ Understood & translated live.",
                isSampleResponse = true,
                quickReplies = if (isBn) listOf("বাক্যে ব্যবহার দেখান", "আরও বলো", "কুইজ দিন")
                else listOf("Use in a sentence", "Quiz me")
            )
        }

        return TutorResponse(
            spokenText = "Tell me more!",
            nativeExplanation = if (isBn) {
                "আমি আপনার কথাটি শুনছি$nameTag (\"$userText\")! আরও একটু বিস্তারিত বলুন, অথবা কোনো তথ্য জানতে চাইলে বিষয়ের নাম লিখে দিন।"
            } else {
                "I hear you (\"$userText\")! Tell me a bit more or ask any topic."
            },
            phrases = emptyList(),
            practicePrompt = if (isBn) "আপনি যা বলতে চান, স্বাচ্ছন্দ্যে লিখুন বা বলুন!" else "Share whatever is on your mind!",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("বাংলাদেশ সম্পর্কে বলো", "এখন কয়টা বাজে?", "কুইজ দিন") else listOf("Tell me about Earth", "Quiz me")
        )
    }

    // -------------------------------------------------------------------------
    // TARGET LANGUAGE CONVERSATION + REAL GRAMMAR ANALYSIS
    // -------------------------------------------------------------------------

    private suspend fun handleTargetConversationReply(
        settings: UserSettings,
        userText: String,
        turnCount: Int
    ): TutorResponse {
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val lower = userText.lowercase()

        val grammarCheck = if (target.code == "en") analyzeEnglishGrammar(userText, isBn) else null
        val liveUserMeaning = if (settings.studyInNativeLanguage) {
            lookupService.translate(userText, target.code, native.code)?.translatedText
        } else null

        val keyword = pickDictionaryKeyword(userText)
        val dictBonus = keyword?.let { lookupService.lookupEnglishWord(it) }
        val dynamicReplyEn = buildDynamicConversationalReply(lower, turnCount)
        val dynamicReplyBn = lookupService.translate(dynamicReplyEn, "en", native.code)?.translatedText
            ?: if (isBn) "দারুণ বলেছেন! চলুন আপনার এই বিষয়টা নিয়েই আরেকটু কথা বলি।" else dynamicReplyEn

        memory.lastTopic = keyword ?: userText.take(35)
        memory.lastSimpleSummary = liveUserMeaning ?: dynamicReplyBn

        val phrases = mutableListOf<VocabPhrase>()
        if (!liveUserMeaning.isNullOrBlank()) {
            phrases.add(
                VocabPhrase(
                    targetText = userText,
                    pronunciation = if (isBn) "আপনার বাক্যের বাংলা অর্থ" else "Meaning of your sentence",
                    nativeMeaning = liveUserMeaning
                )
            )
        }
        if (dictBonus != null) {
            val defBn = lookupService.translate(dictBonus.definition, "en", native.code)?.translatedText
            phrases.add(
                VocabPhrase(
                    targetText = "${dictBonus.word} ${dictBonus.phonetic}".trim(),
                    pronunciation = dictBonus.partOfSpeech.ifBlank { "Vocabulary spotlight" },
                    nativeMeaning = defBn ?: dictBonus.definition
                )
            )
        }
        val followSuggestion = buildContextualContinuation(lower)
        val followBn = lookupService.translate(followSuggestion, "en", native.code)?.translatedText
            ?: if (isBn) "কথা এগিয়ে নিতে এটি বলতে পারেন।" else "Natural way to continue."
        phrases.add(
            VocabPhrase(
                targetText = followSuggestion,
                pronunciation = if (isBn) "এরপর যা বলতে পারেন" else "Natural next line",
                nativeMeaning = followBn
            )
        )

        val feedbackBanner = grammarCheck ?: if (isBn) {
            if (liveUserMeaning != null) "✅ চমৎকার বাক্য! অর্থ: \"$liveUserMeaning\""
            else "✅ দারুণ বলেছেন! আপনার বাক্যটি পরিষ্কার ও স্বাভাবিক।"
        } else {
            "✅ Great phrasing! Clear and natural."
        }

        val headerText = if (settings.studyInNativeLanguage && isBn) {
            "$dynamicReplyEn\n\n🇧🇩 $dynamicReplyBn"
        } else {
            dynamicReplyEn
        }

        return TutorResponse(
            spokenText = dynamicReplyEn,
            nativeExplanation = headerText,
            phrases = phrases,
            practicePrompt = if (isBn) "উপরের প্রশ্নের উত্তর দিন, অথবা যেকোনো তথ্য বা শব্দের অর্থ জানতে জিজ্ঞেস করুন!" else "Reply naturally or ask any question!",
            grammarFeedback = feedbackBanner,
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("বাক্যে ব্যবহার দেখান", "কুইজ দিন", "Past Tense শেখান")
            else listOf("Use in a sentence", "Quiz me", "Teach Past Tense")
        )
    }

    private suspend fun handleRolePlayReply(
        settings: UserSettings,
        scenario: RolePlayScenario,
        userText: String
    ): TutorResponse {
        val isBn = settings.nativeLanguageCode == "bn"
        val grammarCheck = if (settings.targetLanguageCode == "en") analyzeEnglishGrammar(userText, isBn) else null
        val lower = userText.lowercase()

        val replyEn = when (scenario.id) {
            "cafe_cooking" -> when {
                lower.contains("coffee") || lower.contains("latte") || lower.contains("tea") ->
                    "Great choice! Would you like it hot or iced, and would you care for a warm pastry with it?"
                lower.contains("spice") || lower.contains("ingredient") || lower.contains("cook") ->
                    "We use cardamom, cinnamon, and roasted vanilla! Do you enjoy cooking spiced dishes at home too?"
                lower.contains("bill") || lower.contains("check") || lower.contains("pay") ->
                    "Of course! Your total comes to six dollars and fifty cents. Will you be paying by card or cash?"
                else -> "Sounds delicious! Coming right up. Anything else I can prepare for you today?"
            }
            "airport_travel" -> when {
                lower.contains("window") || lower.contains("seat") || lower.contains("aisle") ->
                    "I found a great window seat in row 14A! Do you have any luggage to check in today?"
                lower.contains("gate") || lower.contains("board") || lower.contains("time") ->
                    "Your flight departs from Gate B12, and boarding begins in 40 minutes. Have a pleasant journey!"
                else -> "Thank you! Everything looks confirmed on your booking. Would you prefer a window or aisle seat?"
            }
            "job_interview" -> when {
                lower.contains("strength") || lower.contains("app") || lower.contains("experience") || lower.contains("project") ->
                    "That's very impressive! Could you tell me about a tough challenge you solved on that project?"
                lower.contains("90 days") || lower.contains("team") || lower.contains("role") ->
                    "Great question! In the first 90 days, we'd love you to ship a key user feature and collaborate closely with design."
                else -> "Thank you for sharing that! What motivates you most about joining our team?"
            }
            else -> when {
                lower.contains("size") || lower.contains("medium") || lower.contains("large") || lower.contains("color") ->
                    "Yes, we have medium in navy blue and olive green! Would you like to try one on?"
                lower.contains("discount") || lower.contains("price") || lower.contains("much") ->
                    "If you pick two items today, I can offer you 15% off the bundle. How does that sound?"
                else -> "Lovely choice! That fabric is very comfortable. Would you like to see matching accessories too?"
            }
        }

        val replyBn = if (settings.studyInNativeLanguage) {
            lookupService.translate(replyEn, "en", settings.nativeLanguageCode)?.translatedText ?: ""
        } else ""
        val nextSuggestions = TutorCurriculumBank.rolePlayStarterPhrases(settings.targetLanguageCode, settings.nativeLanguageCode, scenario.id)

        return TutorResponse(
            spokenText = replyEn,
            nativeExplanation = if (isBn && replyBn.isNotBlank()) "$replyEn\n\n🇧🇩 $replyBn" else replyEn,
            phrases = nextSuggestions,
            practicePrompt = if (isBn) "রোল-প্লে চালিয়ে যেতে উত্তর দিন!" else "Reply in character to continue!",
            grammarFeedback = grammarCheck ?: if (isBn) "✅ চমৎকার রোল-প্লে উত্তর!" else "✅ Great in-character reply!",
            isSampleResponse = true,
            quickReplies = nextSuggestions.map { it.targetText }
        )
    }

    // -------------------------------------------------------------------------
    // GRAMMAR & INTERACTIVE QUIZ HANDLERS
    // -------------------------------------------------------------------------

    private fun buildGrammarLessonResponse(settings: UserSettings, lesson: GrammarTopicLesson): TutorResponse {
        val isBn = settings.nativeLanguageCode == "bn"
        memory.lastTopic = lesson.titleBn
        memory.lastSimpleSummary = if (isBn) lesson.summaryBn else lesson.summaryEn
        return TutorResponse(
            spokenText = lesson.phrases.first().targetText,
            nativeExplanation = if (isBn) "📖 ${lesson.titleBn}\n\n${lesson.summaryBn}" else "📖 ${lesson.titleEn}\n\n${lesson.summaryEn}",
            phrases = lesson.phrases,
            practicePrompt = if (isBn) "এই নিয়ম ব্যবহার করে নিজে একটি ইংরেজি বাক্য লিখুন, অথবা নিচের আরেকটি টপিক বেছে নিন!" else "Try writing a sentence with this rule, or pick another topic!",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) lesson.followUpRepliesBn else lesson.followUpRepliesEn
        )
    }

    private fun startNextQuizQuestion(settings: UserSettings, feedbackPrefix: String?): TutorResponse {
        val isBn = settings.nativeLanguageCode == "bn"
        val pool = TutorCurriculumBank.quizBank
        val question = pool[quizCursor % pool.size]
        quizCursor++
        activeQuizQuestion = question

        val body = if (isBn) "🎯 কুইজ প্রশ্ন (${question.level.cefr}):\n${question.questionBn}"
        else "🎯 Quick Quiz (${question.level.cefr}):\n${question.questionEn}"

        return TutorResponse(
            spokenText = if (isBn) question.questionBn else question.questionEn,
            nativeExplanation = body,
            phrases = emptyList(),
            practicePrompt = if (isBn) "নিচের সঠিক অপশনে ট্যাপ করুন:" else "Tap the correct answer below:",
            grammarFeedback = feedbackPrefix,
            isSampleResponse = true,
            quickReplies = question.options
        )
    }

    private fun evaluateQuizAnswer(settings: UserSettings, question: QuizQuestion, userReply: String): TutorResponse {
        val isBn = settings.nativeLanguageCode == "bn"
        val selectedIdx = parseQuizOptionIndex(userReply, question)
        val correctOption = question.options[question.correctIndex]
        val isCorrect = selectedIdx == question.correctIndex

        val feedback = if (isCorrect) {
            if (isBn) "🎉 একদম সঠিক উত্তর! ($correctOption) — ${question.explanationBn}"
            else "🎉 Spot on! ($correctOption) — ${question.explanationEn}"
        } else {
            if (isBn) "💡 কাছাকাছি গেছেন! সঠিক উত্তর হলো $correctOption — ${question.explanationBn}"
            else "💡 Good try! The right answer is $correctOption — ${question.explanationEn}"
        }

        return startNextQuizQuestion(settings, feedbackPrefix = feedback)
    }

    // -------------------------------------------------------------------------
    // PARSING & GRAMMAR UTILITIES
    // -------------------------------------------------------------------------

    private fun extractMeaningQuery(raw: String, module: LearningModule): String? {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase()

        val bnPatterns = listOf(
            Regex("""(?i)(.+?)\s*(?:এর\s*)?(?:মানে\s*কি|মানে\s*কী|মানে|অর্থ\s*কি|অর্থ\s*কী|বাংলা\s*কি|বাংলা\s*কী|বাংলা\s*অর্থ|ইংরেজি\s*কি|ইংরেজি\s*কী|ইংরেজি\s*অর্থ|অনুবাদ\s*করো|অনুবাদ\s*করুন|অনুবাদ|mane\s*ki|ortho\s*ki)\??$"""),
            Regex("""(?i)^(?:মানে\s*কি|অর্থ\s*কি|অনুবাদ\s*করো|অনুবাদ)\s*[:\-]?\s*(.+)$""")
        )
        for (regex in bnPatterns) {
            val match = regex.find(trimmed)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                if (extracted.isNotEmpty()) return extracted
            }
        }

        val enPatterns = listOf(
            Regex("""(?i)^(?:what\s+does|what\s+is\s+the\s+meaning\s+of|meaning\s+of|define|translate|how\s+do\s+you\s+say|how\s+to\s+say)\s+['"]?(.+?)['"]?(?:\s+mean|\s+in\s+\w+)?\??$"""),
            Regex("""(?i)^['"]?(.+?)['"]?\s+(?:meaning|definition|in\s+bengali|in\s+bangla|in\s+english|translate)\??$""")
        )
        for (regex in enPatterns) {
            val match = regex.find(trimmed)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                if (extracted.isNotEmpty()) return extracted
            }
        }

        if (module == LearningModule.TRANSLATE) return trimmed
        if (module == LearningModule.VOCABULARY && trimmed.split(Regex("\\s+")).size <= 3 && !lower.contains("?")) return trimmed
        return null
    }

    private fun detectGrammarTopic(lower: String, module: LearningModule): GrammarTopicLesson? {
        val bank = TutorCurriculumBank.grammarLessons
        return when {
            lower.contains("past tense") || lower.contains("অতীত") -> bank.first { it.key == "past" }
            lower.contains("future tense") || lower.contains("ভবিষ্যৎ") -> bank.first { it.key == "future" }
            lower.contains("present tense") || lower.contains("বর্তমান") -> bank.first { it.key == "present" }
            lower.contains("preposition") || lower.contains("in/on/at") -> bank.first { it.key == "preposition" }
            lower.contains("modal") || lower.contains("could/would") -> bank.first { it.key == "modals" }
            lower.contains("গ্রামার") || lower.contains("টেন্স") || lower == "grammar" || lower == "tense" -> bank.first { it.key == "present" }
            module == LearningModule.GRAMMAR && (lower.contains("next") || lower.contains("আরও") || lower.contains("অন্য")) -> bank[1]
            else -> null
        }
    }

    private fun analyzeEnglishGrammar(sentence: String, isBn: Boolean): String? {
        val lower = sentence.lowercase().trim()
        return when {
            Regex("""\bi\s+am\s+(go|eat|come|work|sleep|play|study|like|want|know)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'I am + verb' বললে verb-এর সাথে '-ing' যোগ হয় (যেমন: I am going), অথবা সরাসরি 'I go / I like' বলুন।"
                else "💡 Grammar tip: Use '-ing' after 'I am' (I am going), or drop 'am' for habits (I go / I like)."
            Regex("""\bi\s+am\s+agree\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'I am agree' নয় — সরাসরি 'I agree' বলুন!" else "💡 Grammar tip: Say 'I agree' instead of 'I am agree'."
            Regex("""\b(did|didn't)\s+(went|ate|came|saw|did|bought|made|took)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'did' বা 'didn't' এর পর verb-এর মূল রূপ (go, eat, come, see) বসে!" else "💡 Grammar tip: Always use the base verb after 'did' or 'didn't'."
            Regex("""\b(he|she)\s+(go|eat|work|like|have|want|live)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: He/She হলে বর্তমান কালে verb-এ s/es বা has বসে (He goes, She likes, She has)।" else "💡 Grammar tip: Add -s/-es with He/She (He goes, She has)."
            Regex("""\bi\s+(no|not)\s+(know|have|like|want|understand)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: না-বোধক বাক্যে 'don't' ব্যবহার করুন (I don't know / I don't have)।" else "💡 Grammar tip: Use 'don't' for negatives (I don't know)."
            Regex("""\bvery\s+(like|love|enjoy)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'very like' নয়, 'really like' বললে একদম স্বাভাবিক শোনায়!" else "💡 Grammar tip: Say 'really like' instead of 'very like'."
            Regex("""\bmore\s+(better|faster|easier|bigger|cheaper)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'more better' নয় — শুধু 'better' বা 'much better' বলুন।" else "💡 Grammar tip: Say 'better' or 'much better'."
            Regex("""\byesterday\s+i\s+(go|eat|see|come|buy)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'Yesterday' অতীত কাল, তাই verb-এর Past রূপ (went, ate, saw) ব্যবহার করুন।" else "💡 Grammar tip: Use past tense with 'yesterday' (went, ate, saw)."
            else -> null
        }
    }

    private fun buildCommonExpressionTip(lower: String, isBn: Boolean): VocabPhrase? = when {
        lower.contains("not sure if you") -> VocabPhrase(
            targetText = "Not sure if you heard / know / are free...",
            pronunciation = if (isBn) "দৈনন্দিন কথোপকথনে ব্যবহার" else "Everyday conversational pattern",
            nativeMeaning = if (isBn) "কাউকে ভদ্র ও সহজ ভঙ্গিতে কোনো খবর জানাতে বা কিছু জিজ্ঞেস করতে বাক্যের শুরুতে 'Not sure if you...' বলা হয় (যেমন: Not sure if you're free today = জানি না আজ আপনি ফ্রি আছেন কিনা)।"
            else "Casual, polite starter meaning 'I'm not certain whether you...'"
        )
        lower.contains("take it easy") -> VocabPhrase(
            targetText = "Take it easy!",
            pronunciation = "টেক ইট ইজি!",
            nativeMeaning = if (isBn) "'চিন্তা করো না / শান্ত থাকো' অথবা বিদায় নেওয়ার সময় 'ভালো থেকো' বোঝাতে ব্যবহৃত হয়।"
            else "Used to say 'relax, don't stress' or casually 'goodbye'."
        )
        else -> null
    }

    private fun buildDynamicConversationalReply(lower: String, turnCount: Int): String = when {
        lower.contains("food") || lower.contains("eat") || lower.contains("cook") || lower.contains("coffee") || lower.contains("tea") ->
            "That sounds delicious! Do you usually make it yourself or enjoy going out for it?"
        lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("interview") || lower.contains("developer") ->
            "That sounds like meaningful work! What part of it do you enjoy the most?"
        lower.contains("travel") || lower.contains("trip") || lower.contains("visit") || lower.contains("country") || lower.contains("airport") ->
            "Traveling is so rewarding! Which destination would you love to visit next?"
        lower.contains("learn") || lower.contains("english") || lower.contains("study") || lower.contains("practice") ->
            "I love your dedication! Which skill do you want to focus on today — speaking, vocabulary, or grammar?"
        lower.contains("family") || lower.contains("friend") || lower.contains("home") ->
            "Spending quality time with loved ones means so much. What do you enjoy doing together?"
        else -> when (turnCount % 3) {
            0 -> "That's really interesting! Could you tell me a little more about it?"
            1 -> "I see what you mean! What made you think about that today?"
            else -> "Great point! What are your thoughts or next plans around that?"
        }
    }

    private fun buildContextualContinuation(lower: String): String = when {
        lower.contains("learn") || lower.contains("english") -> "I want to speak English confidently every day."
        lower.contains("work") || lower.contains("job") -> "I enjoy collaborating with my team on new ideas."
        lower.contains("food") || lower.contains("cook") -> "My favorite homemade dish is spicy and flavorful."
        lower.contains("travel") -> "I love exploring new cities and trying local food."
        else -> "That's why I enjoy talking about this topic."
    }

    private fun pickDictionaryKeyword(englishText: String): String? {
        val stopWords = setOf(
            "the", "and", "for", "that", "with", "this", "from", "your", "have", "are", "was",
            "were", "not", "you", "they", "what", "when", "where", "how", "why", "who", "can",
            "could", "would", "should", "will", "about", "into", "very", "just", "here", "there"
        )
        val tokens = englishText.lowercase().split(Regex("[^a-zA-Z']+")).filter { it.length >= 3 }
        if (tokens.size == 1) return tokens.first()
        return tokens.firstOrNull { it !in stopWords } ?: tokens.maxByOrNull { it.length }
    }

    private fun isPredominantlyNativeScript(text: String, nativeCode: String, targetCode: String): Boolean {
        if (nativeCode == targetCode) return false
        val bengaliChars = text.count { it in '\u0980'..'\u09FF' }
        val nonAsciiLetters = text.count { it.isLetter() && it.code > 127 }
        val asciiLetters = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        return if (nativeCode == "bn") bengaliChars > 0 && bengaliChars >= asciiLetters
        else nonAsciiLetters > asciiLetters && targetCode == "en"
    }

    private fun looksLikeQuizAnswer(input: String, question: QuizQuestion): Boolean {
        val trimmed = input.trim()
        val upper = trimmed.uppercase()
        if (upper in setOf("A", "B", "C") || upper.startsWith("A)") || upper.startsWith("B)") || upper.startsWith("C)")) return true
        return question.options.any { it.equals(trimmed, ignoreCase = true) || it.substringAfter(") ").equals(trimmed, ignoreCase = true) }
    }

    private fun parseQuizOptionIndex(input: String, question: QuizQuestion): Int {
        val trimmed = input.trim()
        val upper = trimmed.uppercase()
        if (upper == "A" || upper.startsWith("A)")) return 0
        if (upper == "B" || upper.startsWith("B)")) return 1
        if (upper == "C" || upper.startsWith("C)")) return 2
        return question.options.indexOfFirst { it.equals(trimmed, ignoreCase = true) || it.substringAfter(") ").equals(trimmed, ignoreCase = true) }
    }
}
