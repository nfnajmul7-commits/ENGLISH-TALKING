package com.example.data

import com.example.model.InspireSuggestion
import com.example.model.LanguageLevel
import com.example.model.LearningModule
import com.example.model.RolePlayScenario
import com.example.model.TutorMode
import com.example.model.TutorResponse
import com.example.model.UserSettings
import com.example.model.VocabPhrase
import kotlinx.coroutines.delay

/**
 * Unified AI tutor interface. Backed by [PlaceholderAiTutorRepository] (which integrates live
 * keyless translation & dictionary lookups, grammar analysis, and interactive quizzes) when
 * Firebase AI Logic is not enabled.
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
 * Interactive Tutor powered by live keyless online translation & English dictionary lookups
 * ([LiveLanguageLookupService]), Bengali/English intent understanding, real grammar mistake
 * detection, and 8 structured learning modules. Remains flagged as [isPlaceholderMode] = true
 * so the UI transparently notes that full generative Gemini AI requires Firebase AI Logic.
 */
class PlaceholderAiTutorRepository(
    private val lookupService: LiveLanguageLookupService = LiveLanguageLookupService()
) : AiTutorRepository {

    override val isPlaceholderMode: Boolean = true

    private var activeQuizQuestion: QuizQuestion? = null
    private var quizCursor: Int = 0
    private var lastLookedUpPhrase: String? = null

    override suspend fun generateLessonIntro(
        settings: UserSettings,
        mode: TutorMode,
        topic: String,
        module: LearningModule
    ): TutorResponse {
        delay(220)
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
                "নমস্কার! আজ আমরা ${target.nativeName} (${target.englishName}) ভাষার ${level.bnLabel} (${level.cefr}) স্তরের কথোপকথন অনুশীলন করব। আপনি যেকোনো প্রশ্ন করতে পারেন, বাংলায় বা ইংরেজিতে কথা বলুন!"
            } else {
                "Welcome! Let's practice ${level.label} (${level.cefr}) ${target.englishName} conversation. Say anything or ask a word meaning anytime!"
            }

            LearningModule.TRANSLATE -> if (isBn) {
                "🔍 অর্থ ও অনুবাদ মোড চালু হয়েছে! যেকোনো ইংরেজি শব্দ বা বাক্য লিখে (যেমন: \"not sure if you মানে কি\") অথবা বাংলা বাক্য লিখুন — আমি অনলাইন ডিকশনারি ও অনুবাদ থেকে সঠিক অর্থ, উচ্চারণ ও উদাহরণ জানিয়ে দেব।"
            } else {
                "🔍 Meaning & Translate Mode is ready! Type any ${target.englishName} or ${native.englishName} word or sentence for a live translation, phonetics, and real usage examples."
            }

            LearningModule.GRAMMAR -> {
                val first = TutorCurriculumBank.grammarLessons.first()
                if (isBn) "📖 ${first.titleBn}: ${first.summaryBn}"
                else "📖 ${first.titleEn}: ${first.summaryEn}"
            }

            LearningModule.VOCABULARY -> if (isBn) {
                "🔤 আজকের ${level.bnLabel} (${level.cefr}) শব্দভান্ডার! যেকোনো নতুন ইংরেজি শব্দ টাইপ করলে আমি তার বাংলা অর্থ, উচ্চারণ (IPA) ও সমার্থক শব্দ বলে দেব।"
            } else {
                "🔤 Today's ${level.label} (${level.cefr}) Vocabulary! Type any word to get its live definition, phonetics, and synonyms."
            }

            LearningModule.PRONUNCIATION -> if (isBn) {
                "🗣️ উচ্চারণ অনুশীলন! ইংরেজির কিছু শব্দের বানান আর উচ্চারণ আলাদা হয়। প্রতিটি কার্ডে ট্যাপ করে মধুর কণ্ঠে শুনুন এবং নিজে বলার চেষ্টা করুন:"
            } else {
                "🗣️ Pronunciation Studio! Tap any card to listen closely to natural stress and rhythm, then repeat aloud."
            }

            LearningModule.OFFICE -> if (isBn) {
                "💼 অফিস, মিটিং ও জব ইন্টারভিউয়ের জন্য স্মার্ট ইংরেজি বাক্য! নিচের বাক্যগুলো শিখুন অথবা আপনার নিজের কাজের কথা ইংরেজিতে লিখে প্র্যাকটিস করুন:"
            } else {
                "💼 Workplace & Interview expressions! Practice introducing your skills or ask how to phrase a professional sentence."
            }

            LearningModule.TRAVEL -> if (isBn) {
                "✈️ এয়ারপোর্ট, হোটেল ও রেস্তোরাঁয় ব্যবহার করার মতো জরুরি ইংরেজি বাক্য! এগুলো শুনুন অথবা ভ্রমণের যেকোনো পরিস্থিতি লিখে অনুশীলন করুন:"
            } else {
                "✈️ Essential Travel & Dining expressions! Practice ordering, asking directions, or checking in."
            }

            LearningModule.QUIZ -> ""
        }

        val quickReplies = when (module) {
            LearningModule.CONVERSATION -> if (isBn) listOf(
                "not sure if you মানে কি?",
                "My name is Najmul",
                "আমি ইংরেজি শিখতে চাই",
                "কুইজ দিন"
            ) else listOf("Hello! How are you?", "What does 'curious' mean?", "Teach Past Tense", "Quiz me")

            LearningModule.TRANSLATE -> if (isBn) listOf(
                "not sure if you মানে কি",
                "take it easy মানে কি",
                "আমি ইংরেজি শিখতে চাই",
                "resilient meaning"
            ) else listOf("What does 'not sure if you' mean?", "Meaning of resilient", "How to say 'I am grateful'")

            LearningModule.GRAMMAR -> if (isBn) TutorCurriculumBank.grammarLessons.first().followUpRepliesBn
            else TutorCurriculumBank.grammarLessons.first().followUpRepliesEn

            LearningModule.VOCABULARY -> if (isBn) listOf(
                "serendipity meaning",
                "consistent মানে কি",
                "কুইজ দিন",
                "বাক্যে ব্যবহার দেখান"
            ) else listOf("Meaning of serendipity", "Meaning of consistent", "Quiz me")

            LearningModule.PRONUNCIATION -> if (isBn) listOf(
                "entrepreneur meaning",
                "schedule মানে কি",
                "কুইজ দিন"
            ) else listOf("Meaning of entrepreneur", "Quiz me")

            LearningModule.OFFICE -> if (isBn) listOf(
                "I work as a software developer",
                "ফলো-আপ ইমেইল ইংরেজি কি",
                "Polite Modals (Could/Would)"
            ) else listOf("Tell me about your strengths", "Polite Modals", "Quiz me")

            LearningModule.TRAVEL -> if (isBn) listOf(
                "বিলটি দেবেন ইংরেজি কি",
                "I'd like a window seat",
                "ভ্রমণের আরও বাক্য"
            ) else listOf("Could I get a window seat?", "Where is the nearest metro?")

            LearningModule.QUIZ -> emptyList()
        }

        val followUp = if (isBn) {
            when (module) {
                LearningModule.TRANSLATE -> "যেকোনো ইংরেজি বা বাংলা শব্দ/বাক্য লিখে পাঠান — আমি এখনই সঠিক অর্থ খুঁজে দেব!"
                LearningModule.GRAMMAR -> "নিচের যেকোনো গ্রামার টপিকে ট্যাপ করুন, অথবা একটি ইংরেজি বাক্য লিখুন — আমি ভুল থাকলে ঠিক করে দেব!"
                else -> "নিচের সাজেশনে ট্যাপ করুন, অথবা Speak/Type দিয়ে নিজের মতো যেকোনো প্রশ্ন বা কথা বলুন!"
            }
        } else {
            "Tap a quick option below, or use Speak / Type to ask anything or practice a sentence!"
        }

        return TutorResponse(
            spokenText = if (isBn) "ঠিক আছে, চলুন শুরু করি।" else "Alright, let's get started!",
            nativeExplanation = if (settings.studyInNativeLanguage) openerNative else "Let's focus on ${module.label} (${level.label}).",
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
        delay(240)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val spokenText = TutorCurriculumBank.rolePlayOpeningLine(target.code, scenario.id)
        val phrases = TutorCurriculumBank.rolePlayStarterPhrases(target.code, native.code, scenario.id)

        val nativeExplanation = if (settings.studyInNativeLanguage) {
            if (isBn) {
                "রোল-প্লে শুরু হলো! (${scenario.title}) আমি এখন আপনার ${scenario.aiRoleTitle}। আমি বললাম: \"$spokenText\""
            } else {
                "Let's start ${scenario.title}! I'm your ${scenario.aiRoleTitle}: \"$spokenText\""
            }
        } else {
            spokenText
        }

        val quickReplies = phrases.map { it.targetText }

        return TutorResponse(
            spokenText = spokenText,
            nativeExplanation = nativeExplanation,
            phrases = phrases,
            practicePrompt = if (isBn) "নিচের যেকোনো একটি বাক্যে ট্যাপ করুন অথবা Speak/Type দিয়ে নিজের উত্তর বলুন!" else "Tap a suggested reply or respond in your own words!",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = quickReplies
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
        val isBn = settings.nativeLanguageCode == "bn"

        // 1. Active Quiz grading (if user tapped A)/B)/C) or answered active quiz)
        val pendingQuiz = activeQuizQuestion
        if (pendingQuiz != null && (module == LearningModule.QUIZ || looksLikeQuizAnswer(trimmed, pendingQuiz))) {
            return evaluateQuizAnswer(settings, pendingQuiz, trimmed)
        }

        // 2. Explicit Quiz request from anywhere
        if (lower.contains("কুইজ") || lower == "quiz" || lower.contains("quiz me") || lower.contains("test me") || lower.contains("পরীক্ষা")) {
            return startNextQuizQuestion(settings, feedbackPrefix = null)
        }

        // 3. Example sentences for last looked-up phrase
        if ((lower.contains("বাক্যে ব্যবহার") || lower.contains("উদাহরণ") || lower.contains("example sentence") || lower.contains("use in a sentence")) &&
            !lastLookedUpPhrase.isNullOrBlank()
        ) {
            return buildUsageExamplesResponse(settings, lastLookedUpPhrase!!)
        }

        // 4. Grammar / Tense topic request
        val matchedGrammar = detectGrammarTopic(lower, module)
        if (matchedGrammar != null) {
            return buildGrammarLessonResponse(settings, matchedGrammar)
        }

        // 5. Meaning / Translation lookup (e.g., "not sure if you মানে কি", "serendipity meaning", or Translate module)
        val meaningTarget = extractMeaningQuery(trimmed, module)
        if (meaningTarget != null) {
            return handleLiveMeaningOrTranslation(settings, rawUserText = trimmed, query = meaningTarget)
        }

        // 6. Role-Play contextual response (with live translation & real grammar check!)
        if (scenario != null) {
            return handleRolePlayReply(settings, scenario, trimmed, turnCount)
        }

        // 7. User wrote in Bengali (or non-Latin native script while learning English/Latin target)
        if (isPredominantlyNativeScript(trimmed, settings.nativeLanguageCode, settings.targetLanguageCode)) {
            return handleNativeSentenceToTarget(settings, trimmed)
        }

        // 8. Conversational Target Language input (real grammar check + intent understanding + live translation!)
        return handleTargetConversationReply(settings, trimmed, module, turnCount)
    }

    override suspend fun generateInspireHints(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        lastAiMessage: String?,
        module: LearningModule
    ): List<InspireSuggestion> {
        delay(120)
        val targetCode = settings.targetLanguageCode
        val nativeCode = settings.nativeLanguageCode
        if (scenario != null) {
            return TutorCurriculumBank.rolePlayStarterPhrases(targetCode, nativeCode, scenario.id).map {
                InspireSuggestion(it.targetText, it.nativeMeaning)
            }
        }
        return TutorCurriculumBank.modulePhrases(module, targetCode, nativeCode, settings.level, "Daily Life")
            .take(3)
            .map { InspireSuggestion(it.targetText, it.nativeMeaning) }
    }

    // -------------------------------------------------------------------------
    // LIVE TRANSLATION & DICTIONARY HANDLER
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

        val queryIsNativeScript = isPredominantlyNativeScript(cleanQuery, native.code, target.code)
        val explicitToTarget = rawUserText.contains("ইংরেজি") || rawUserText.lowercase().contains("in english") ||
            rawUserText.lowercase().contains("how to say") || rawUserText.lowercase().contains("to ${target.englishName.lowercase()}")

        val fromCode = if (queryIsNativeScript || explicitToTarget) native.code else target.code
        val toCode = if (fromCode == native.code) target.code else native.code

        val translation = lookupService.translate(cleanQuery, fromCode, toCode)

        // Lookup dictionary entry for single English word or key word inside a short English phrase
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

            phrases.add(
                VocabPhrase(
                    targetText = primaryTarget,
                    pronunciation = phoneticLabel,
                    nativeMeaning = primaryNative
                )
            )

            // Add nuanced conversational explanation if it's a known common English expression
            val nuanceNote = buildCommonExpressionTip(cleanQuery.lowercase(), isBn)
            if (nuanceNote != null) {
                phrases.add(nuanceNote)
            }

            // Add live dictionary example sentence translated into Bengali/Native!
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

            // Add synonyms if available
            if (dictEntry != null && dictEntry.synonyms.isNotEmpty()) {
                val syns = dictEntry.synonyms.joinToString(", ")
                phrases.add(
                    VocabPhrase(
                        targetText = syns,
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

            val spokenSummary = if (fromCode == target.code) cleanQuery else translation.translatedText

            return TutorResponse(
                spokenText = spokenSummary,
                nativeExplanation = header,
                phrases = phrases,
                practicePrompt = if (isBn) {
                    "এই বাক্য/শব্দটি কি বাস্তব উদাহরণে দেখতে চান, নাকি অন্য কোনো শব্দের অর্থ জানতে চান?"
                } else {
                    "Would you like to see this used in example sentences, or ask another word?"
                },
                grammarFeedback = if (isBn) "অনলাইন অনুবাদ ও ডিকশনারি থেকে লাইভ উত্তর সংগ্রহ করা হয়েছে।" else "Live answer fetched from online translation & dictionary.",
                isSampleResponse = true,
                quickReplies = if (isBn) listOf(
                    "বাক্যে ব্যবহার দেখান",
                    "কুইজ দিন",
                    "Past Tense শেখান"
                ) else listOf("Use in a sentence", "Quiz me", "Teach Past Tense")
            )
        } else {
            // Network unavailable fallback (transparent, honest, helpful!)
            val offlineMsg = if (isBn) {
                "দুঃখিত, এই মুহূর্তে অনলাইন অনুবাদ সার্ভারে সংযোগ পাওয়া যায়নি («$cleanQuery»)। আপনার ইন্টারনেট চালু আছে কিনা দেখে আবার চেষ্টা করুন।"
            } else {
                "Sorry, I couldn't reach the online translation service right now for \"$cleanQuery\". Please check your internet connection and try again."
            }
            return TutorResponse(
                spokenText = cleanQuery,
                nativeExplanation = offlineMsg,
                phrases = emptyList(),
                practicePrompt = if (isBn) "আবার চেষ্টা করুন অথবা নিচের অফলাইন গ্রামার/কুইজ অপশন বেছে নিন।" else "Try again or explore Grammar / Quiz below.",
                grammarFeedback = null,
                isSampleResponse = true,
                quickReplies = if (isBn) listOf(rawUserText, "গ্রামার ও টেন্স", "কুইজ দিন") else listOf(rawUserText, "Teach Past Tense", "Quiz me")
            )
        }
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
                    "Could you explain the word '$clean' with another example?"
                ).distinct().take(3)
            }
            else -> listOf(
                "$clean — could you tell me what you think?",
                "I often hear people say, \"$clean\" in everyday English.",
                "Let's practice saying \"$clean\" clearly and naturally."
            )
        }

        val vocabList = examples.map { ex ->
            val bn = lookupService.translate(ex, "en", settings.nativeLanguageCode)?.translatedText
                ?: if (isBn) "বাস্তব কথোপকথনে ব্যবহারের উদাহরণ।" else "Natural everyday example."
            VocabPhrase(targetText = ex, pronunciation = "", nativeMeaning = bn)
        }

        return TutorResponse(
            spokenText = examples.first(),
            nativeExplanation = if (isBn) {
                "নিচে «$clean» ব্যবহার করে ৩টি বাস্তব ইংরেজি বাক্য ও বাংলা অর্থ দেওয়া হলো — যেকোনো বাক্যে ট্যাপ করে উচ্চারণ শুনুন:"
            } else {
                "Here are 3 real-life sentences using \"$clean\":"
            },
            phrases = vocabList,
            practicePrompt = if (isBn) "এবার আপনি নিজে «$clean» দিয়ে একটি ছোট ইংরেজি বাক্য বানিয়ে বলুন বা লিখুন!" else "Now try making your own sentence with \"$clean\"!",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("আরেকটি শব্দের অর্থ জানব", "কুইজ দিন", "Past Tense শেখান")
            else listOf("Quiz me", "Teach Past Tense")
        )
    }

    // -------------------------------------------------------------------------
    // NATIVE (BENGALI) MESSAGE -> LIVE TARGET LANGUAGE TEACHING
    // -------------------------------------------------------------------------

    private suspend fun handleNativeSentenceToTarget(
        settings: UserSettings,
        userText: String
    ): TutorResponse {
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val translated = lookupService.translate(userText, native.code, target.code)
        val targetSentence = translated?.translatedText

        if (targetSentence != null) {
            lastLookedUpPhrase = targetSentence
            val phrases = mutableListOf(
                VocabPhrase(
                    targetText = targetSentence,
                    pronunciation = if (isBn) "আপনার কথার সঠিক ${target.englishName}" else "Your sentence in ${target.englishName}",
                    nativeMeaning = userText
                )
            )

            // Add a helpful conversational continuation based on what they said
            val continuationTarget = buildContextualContinuation(targetSentence.lowercase())
            val continuationNative = lookupService.translate(continuationTarget, "en", native.code)?.translatedText
                ?: if (isBn) "এরপর কথা চালিয়ে যেতে এটি বলতে পারেন।" else "A natural way to continue the conversation."
            phrases.add(
                VocabPhrase(
                    targetText = continuationTarget,
                    pronunciation = if (isBn) "এর উত্তরে / পরবর্তী বাক্য" else "Natural follow-up",
                    nativeMeaning = continuationNative
                )
            )

            return TutorResponse(
                spokenText = targetSentence,
                nativeExplanation = if (isBn) {
                    "দারুণ! আপনি বললেন: \"$userText\"। ${target.nativeName} (${target.englishName}) ভাষায় এটি সুন্দরভাবে এভাবে বলবেন:"
                } else {
                    "Here is how to say \"$userText\" naturally in ${target.englishName}:"
                },
                phrases = phrases,
                practicePrompt = if (isBn) {
                    "এখন উপরের প্রথম বাক্যটি Speak বাটনে ট্যাপ করে বলার অনুশীলন করুন, অথবা আরও কিছু জিজ্ঞেস করুন!"
                } else {
                    "Try speaking that sentence aloud, or ask me anything else!"
                },
                grammarFeedback = if (isBn) "অনলাইন অনুবাদ ব্যবহার করে আপনার বাক্যটি ${target.englishName}-এ সাজিয়ে দেওয়া হয়েছে।" else "Translated live to ${target.englishName}.",
                isSampleResponse = true,
                quickReplies = if (isBn) listOf("বাক্যে ব্যবহার দেখান", "কুইজ দিন", "Present Tense শেখান")
                else listOf("Use in a sentence", "Quiz me", "Teach Present Tense")
            )
        }

        return TutorResponse(
            spokenText = "Let's practice together!",
            nativeExplanation = if (isBn) {
                "আমি আপনার কথাটি বুঝেছি (\"$userText\"), তবে অনলাইন অনুবাদে সংযোগ পাওয়া যায়নি। ইন্টারনেট চালু করে আবার চেষ্টা করুন, অথবা ইংরেজিতে একটি বাক্য লিখুন!"
            } else {
                "I received \"$userText\", but the live translator is currently offline. Please check internet or type in ${target.englishName}."
            },
            phrases = emptyList(),
            practicePrompt = if (isBn) "নিচের যেকোনো একটি অপশন বেছে নিন:" else "Pick a topic below:",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) listOf("Present Tense শেখান", "Past Tense শেখান", "কুইজ দিন")
            else listOf("Teach Present Tense", "Quiz me")
        )
    }

    // -------------------------------------------------------------------------
    // TARGET LANGUAGE CONVERSATION + REAL GRAMMAR ANALYSIS
    // -------------------------------------------------------------------------

    private suspend fun handleTargetConversationReply(
        settings: UserSettings,
        userText: String,
        module: LearningModule,
        turnCount: Int
    ): TutorResponse {
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val lower = userText.lowercase()

        // 1. Real grammar analysis
        val grammarCheck = if (target.code == "en") analyzeEnglishGrammar(userText, isBn) else null

        // 2. Live Bengali meaning of what the user actually said (so user sees Aria understood them!)
        val liveUserMeaning = if (settings.studyInNativeLanguage) {
            lookupService.translate(userText, target.code, native.code)?.translatedText
        } else null

        // 3. Detect specific conversational intents
        val nameRegex = Regex("(?:my name is|i am|i'm|call me)\\s+([a-zA-Z]{2,20})", RegexOption.IGNORE_CASE)
        val nameMatch = nameRegex.find(userText)
        val extractedName = nameMatch?.groupValues?.getOrNull(1)?.takeIf {
            it.lowercase() !in setOf("fine", "good", "happy", "tired", "busy", "ready", "sorry", "sure", "here", "going", "learning", "studying", "working", "from", "not", "very", "okay", "ok", "great")
        }

        val (aiReplyEn, aiReplyBn, suggestedPhrases, quickReplies) = when {
            extractedName != null -> Quad(
                "Nice to meet you, $extractedName! Where are you from, and what do you enjoy doing in your free time?",
                "আপনার সাথে পরিচিত হয়ে খুব ভালো লাগল, $extractedName! আপনি কোথা থেকে বলছেন এবং অবসর সময়ে কী করতে ভালোবাসেন?",
                listOf(
                    VocabPhrase("I'm from Bangladesh, and I love learning new skills.", "আইম ফ্রম বাংলাদেশ...", if (isBn) "আমি বাংলাদেশ থেকে বলছি, আর নতুন দক্ষতা শিখতে ভালোবাসি।" else "Share where you are from and an interest."),
                    VocabPhrase("In my free time, I enjoy watching movies and traveling.", "ইন মাই ফ্রি টাইম...", if (isBn) "অবসর সময়ে আমি মুভি দেখতে ও ঘুরতে ভালোবাসি।" else "Talk about your favorite hobbies.")
                ),
                listOf("I'm from Bangladesh", "I like traveling and tech", "কুইজ দিন")
            )

            lower.contains("how are you") || lower.contains("how's it going") || lower.contains("how are u") -> Quad(
                "I'm doing wonderfully, thank you for asking! How has your day been so far?",
                "আমি খুব ভালো আছি, জিজ্ঞেস করার জন্য ধন্যবাদ! আপনার আজকের দিনটা কেমন যাচ্ছে?",
                listOf(
                    VocabPhrase("My day is going really well, thanks!", "মাই ডে ইজ গোয়িং রিয়েলি ওয়েল!", if (isBn) "আমার দিনটা দারুণ যাচ্ছে, ধন্যবাদ!" else "Positive everyday response."),
                    VocabPhrase("It's been a bit busy, but I'm feeling good.", "ইটস বিন আ বিট বিজি...", if (isBn) "আজ একটু ব্যস্ত ছিলাম, তবে ভালো আছি।" else "Natural way to describe a busy day.")
                ),
                listOf("My day is going great!", "A little busy today", "কুইজ দিন")
            )

            lower.matches(Regex("^(hi|hello|hey|good morning|good afternoon|good evening|assalamu alaikum|salam).*")) -> Quad(
                "Hello there! It's great to talk with you today. What topic would you like to practice — daily conversation, travel, or grammar?",
                "হ্যালো! আজ আপনার সাথে কথা বলতে পেরে খুব ভালো লাগছে। আপনি কী নিয়ে অনুশীলন করতে চান — দৈনন্দিন কথা, ভ্রমণ, নাকি গ্রামার?",
                listOf(
                    VocabPhrase("I want to practice everyday English conversation.", "আই ওয়ান্ট টু প্র্যাকটিস এভরিডে ইংলিশ...", if (isBn) "আমি প্রতিদিনের ইংরেজি কথোপকথন অনুশীলন করতে চাই।" else "State your practice goal."),
                    VocabPhrase("Let's talk about my daily routine.", "লেটস টক অ্যাবাউট মাই ডেইলি রুটিন।", if (isBn) "চলুন আমার প্রতিদিনের রুটিন নিয়ে কথা বলি।" else "Start talking about your day.")
                ),
                listOf("Let's talk about my daily routine", "Past Tense শেখান", "কুইজ দিন")
            )

            lower.contains("i am fine") || lower.contains("i'm fine") || lower.contains("i am good") || lower.contains("i'm good") || lower.contains("going great") -> Quad(
                "That's awesome to hear! Tell me one thing you did today or plan to do later.",
                "শুনে খুব ভালো লাগল! আজ আপনি কী করেছেন বা পরে কী করার পরিকল্পনা আছে, ইংরেজিতে একটি বাক্যে বলুন তো!",
                listOf(
                    VocabPhrase("Today I worked on my goals and practiced English.", "টুডে আই ওয়ার্কড অন মাই গোলস...", if (isBn) "আজ আমি আমার লক্ষ্যে কাজ করেছি এবং ইংরেজি অনুশীলন করেছি।" else "Share what you did today (Past Tense)."),
                    VocabPhrase("Later tonight, I'm going to relax with my family.", "লেটার টুনাইট, আইম গোয়িং টু রিল্যাক্স...", if (isBn) "আজ রাতে আমি পরিবারের সাথে বিশ্রাম নেব।" else "Share an upcoming plan (Future).")
                ),
                listOf("Today I went to work", "I am going to study tonight", "কুইজ দিন")
            )

            lower.contains("thank") || lower.contains("thanks") -> Quad(
                "You're very welcome! I'm always happy to help you grow more confident. What shall we practice next?",
                "আপনাকে স্বাগতম! আপনার ইংরেজি আরও সুন্দর ও আত্মবিশ্বাসী করতে সাহায্য করতে পেরে আমার ভালো লাগছে। এরপর কী শিখবেন?",
                listOf(
                    VocabPhrase("You're welcome! Anytime.", "ইয়োর ওয়েলকাম! এনিটাইম।", if (isBn) "স্বাগতম! যেকোনো সময়।" else "Warm reply to 'Thank you'."),
                    VocabPhrase("I really appreciate your help.", "আই রিয়েলি অ্যাপ্রিশিয়েট ইয়োর হেল্প।", if (isBn) "আপনার সাহায্যের জন্য আমি সত্যিই কৃতজ্ঞ।" else "Polite elevated way to say thanks.")
                ),
                listOf("কুইজ দিন", "Preposition (in/on/at)", "not sure if you মানে কি")
            )

            lower.contains("bye") || lower.contains("goodbye") || lower.contains("see you") -> Quad(
                "Goodbye! You did a great job practicing today. See you in our next session!",
                "বিদায়! আজ আপনি চমৎকার অনুশীলন করেছেন। পরের সেশনে আবার দেখা হবে!",
                listOf(
                    VocabPhrase("Take care and see you soon!", "টেক কেয়ার অ্যান্ড সি ইউ সুন!", if (isBn) "ভালো থাকবেন, শীঘ্রই দেখা হবে!" else "Warm farewell."),
                    VocabPhrase("Have a wonderful rest of your day!", "হ্যাভ আ ওয়ান্ডারফুল রেস্ট অব ইয়োর ডে!", if (isBn) "আপনার বাকি দিনটি খুব সুন্দর কাটুক!" else "Polite goodbye.")
                ),
                listOf("Hello! Let's practice again", "কুইজ দিন")
            )

            else -> {
                // Truly dynamic response for ANY custom English sentence!
                val keyword = pickDictionaryKeyword(userText)
                val dictBonus = keyword?.let { lookupService.lookupEnglishWord(it) }
                val dynamicReplyEn = buildDynamicConversationalReply(lower, turnCount)
                val dynamicReplyBn = lookupService.translate(dynamicReplyEn, "en", native.code)?.translatedText
                    ?: if (isBn) "দারুণ বলেছেন! চলুন আপনার এই বিষয়টা নিয়েই আরেকটু কথা বলি।" else dynamicReplyEn

                val phrases = mutableListOf<VocabPhrase>()
                if (!liveUserMeaning.isNullOrBlank()) {
                    phrases.add(
                        VocabPhrase(
                            targetText = userText,
                            pronunciation = if (isBn) "আপনার বাক্যের অর্থ" else "Meaning of your sentence",
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
                    ?: if (isBn) "কথা এগিয়ে নিতে এই বাক্যটি ব্যবহার করতে পারেন।" else "Natural way to expand your thought."
                phrases.add(
                    VocabPhrase(
                        targetText = followSuggestion,
                        pronunciation = if (isBn) "কথা এগিয়ে নেওয়ার দারুণ উপায়" else "Try expanding with this",
                        nativeMeaning = followBn
                    )
                )

                val bnIntro = if (isBn) {
                    "$dynamicReplyBn"
                } else {
                    dynamicReplyEn
                }
                Quad(
                    dynamicReplyEn,
                    bnIntro,
                    phrases,
                    if (isBn) listOf("বাক্যে ব্যবহার দেখান", "কুইজ দিন", "গ্রামার ও টেন্স")
                    else listOf("Use in a sentence", "Quiz me", "Teach Past Tense")
                )
            }
        }

        val feedbackBanner = grammarCheck ?: if (isBn) {
            if (liveUserMeaning != null) "✅ চমৎকার বাক্য! আপনার কথার অর্থ: \"$liveUserMeaning\""
            else "✅ দারুণ বলেছেন! আপনার বাক্যটি পরিষ্কার ও স্বাভাবিক হয়েছে।"
        } else {
            "✅ Great phrasing! Clear and natural."
        }

        val headerText = if (settings.studyInNativeLanguage && isBn) {
            "$aiReplyEn\n\n🇧🇩 $aiReplyBn"
        } else {
            aiReplyEn
        }

        return TutorResponse(
            spokenText = aiReplyEn,
            nativeExplanation = headerText,
            phrases = suggestedPhrases,
            practicePrompt = if (isBn) "উপরের প্রশ্নের উত্তর ইংরেজিতে দিন, অথবা যেকোনো শব্দের অর্থ জানতে লিখুন!" else "Reply naturally in ${target.englishName}, or ask any word's meaning!",
            grammarFeedback = feedbackBanner,
            isSampleResponse = true,
            quickReplies = quickReplies
        )
    }

    private suspend fun handleRolePlayReply(
        settings: UserSettings,
        scenario: RolePlayScenario,
        userText: String,
        turnCount: Int
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

    private fun buildGrammarLessonResponse(
        settings: UserSettings,
        lesson: GrammarTopicLesson
    ): TutorResponse {
        val isBn = settings.nativeLanguageCode == "bn"
        return TutorResponse(
            spokenText = lesson.phrases.first().targetText,
            nativeExplanation = if (isBn) "📖 ${lesson.titleBn}\n\n${lesson.summaryBn}" else "📖 ${lesson.titleEn}\n\n${lesson.summaryEn}",
            phrases = lesson.phrases,
            practicePrompt = if (isBn) "এই নিয়ম ব্যবহার করে নিজে একটি ইংরেজি বাক্য লিখুন, অথবা নিচের আরেকটি টপিক বেছে নিন!" else "Try writing a sentence with this rule, or pick another grammar topic below!",
            grammarFeedback = null,
            isSampleResponse = true,
            quickReplies = if (isBn) lesson.followUpRepliesBn else lesson.followUpRepliesEn
        )
    }

    private fun startNextQuizQuestion(
        settings: UserSettings,
        feedbackPrefix: String?
    ): TutorResponse {
        val isBn = settings.nativeLanguageCode == "bn"
        val levelQuestions = TutorCurriculumBank.quizBank.filter { it.level == settings.level }
            .ifEmpty { TutorCurriculumBank.quizBank }
        val pool = if (levelQuestions.size >= 2) TutorCurriculumBank.quizBank else TutorCurriculumBank.quizBank
        val question = pool[quizCursor % pool.size]
        quizCursor++
        activeQuizQuestion = question

        val body = if (isBn) {
            "🎯 কুইজ প্রশ্ন (${question.level.cefr}):\n${question.questionBn}"
        } else {
            "🎯 Quick Quiz (${question.level.cefr}):\n${question.questionEn}"
        }

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

    private fun evaluateQuizAnswer(
        settings: UserSettings,
        question: QuizQuestion,
        userReply: String
    ): TutorResponse {
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

        // Immediately serve the next question with the feedback banner!
        return startNextQuizQuestion(settings, feedbackPrefix = feedback)
    }

    // -------------------------------------------------------------------------
    // SMART INTENT & GRAMMAR PARSERS
    // -------------------------------------------------------------------------

    private fun extractMeaningQuery(raw: String, module: LearningModule): String? {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase()

        // Explicit Bengali meaning/translation triggers
        val bnPatterns = listOf(
            Regex("""(?i)(.+?)\s*(?:এর\s*)?(?:মানে\s*কি|মানে\s*কী|অর্থ\s*কি|অর্থ\s*কী|বাংলা\s*কি|বাংলা\s*কী|বাংলা\s*অর্থ|ইংরেজি\s*কি|ইংরেজি\s*কী|ইংরেজি\s*অর্থ|অনুবাদ\s*করো|অনুবাদ\s*করুন|অনুবাদ)\??$"""),
            Regex("""(?i)^(?:মানে\s*কি|অর্থ\s*কি|অনুবাদ\s*করো|অনুবাদ)\s*[:\-]?\s*(.+)$""")
        )
        for (regex in bnPatterns) {
            val match = regex.find(trimmed)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                if (extracted.isNotEmpty()) return extracted
            }
        }

        // Explicit English meaning/translation triggers
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

        // If user is explicitly inside Meaning & Translate or Vocabulary mode, treat any input directly as a lookup!
        if (module == LearningModule.TRANSLATE) {
            return trimmed
        }
        if (module == LearningModule.VOCABULARY && trimmed.split(Regex("\\s+")).size <= 3 && !lower.contains("?")) {
            return trimmed
        }

        return null
    }

    private fun detectGrammarTopic(lower: String, module: LearningModule): GrammarTopicLesson? {
        val bank = TutorCurriculumBank.grammarLessons
        return when {
            lower.contains("past tense") || lower.contains("অতীত") -> bank.first { it.key == "past" }
            lower.contains("future tense") || lower.contains("ভবিষ্যৎ") -> bank.first { it.key == "future" }
            lower.contains("present tense") || lower.contains("বর্তমান") -> bank.first { it.key == "present" }
            lower.contains("preposition") || lower.contains("in/on/at") -> bank.first { it.key == "preposition" }
            lower.contains("modal") || lower.contains("could/would") || lower.contains("polite") -> bank.first { it.key == "modals" }
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
                else "💡 Grammar tip: Use '-ing' after 'I am' (e.g., I am going), or drop 'am' for habits (I go / I like)."

            Regex("""\bi\s+am\s+agree\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'I am agree' নয় — 'agree' নিজেই verb, তাই সরাসরি 'I agree' বলুন!"
                else "💡 Grammar tip: Say 'I agree' instead of 'I am agree'."

            Regex("""\b(did|didn't)\s+(went|ate|came|saw|did|bought|made|took)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'did' বা 'didn't' এর পর সবসময় verb-এর মূল রূপ (go, eat, come, see) বসে!"
                else "💡 Grammar tip: Always use the base verb after 'did' or 'didn't' (e.g., 'did you go', not 'did you went')."

            Regex("""\b(he|she)\s+(go|eat|work|like|have|want|live)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: He/She হলে বর্তমান কালে verb-এর শেষে s/es বা has বসে (যেমন: He goes, She likes, She has)।"
                else "💡 Grammar tip: With He/She, add -s/-es to the present verb (e.g., He goes, She likes, She has)."

            Regex("""\bi\s+(no|not)\s+(know|have|like|want|understand)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: না-বোধক বাক্যে 'don't' ব্যবহার করুন (যেমন: I don't know / I don't have)।"
                else "💡 Grammar tip: Use 'don't' for negatives (e.g., 'I don't know' / 'I don't have')."

            Regex("""\bvery\s+(like|love|enjoy)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'very like' এর বদলে 'really like' বললে একদম নেটিভদের মতো স্বাভাবিক শোনায়!"
                else "💡 Grammar tip: Say 'really like' instead of 'very like' for natural English."

            Regex("""\bmore\s+(better|faster|easier|bigger|cheaper)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'more better' নয় — শুধু 'better' বা 'much better' বলুন।"
                else "💡 Grammar tip: Avoid double comparatives — say 'better' or 'much better' instead of 'more better'."

            Regex("""\byesterday\s+i\s+(go|eat|see|come|buy)\b""").containsMatchIn(lower) ->
                if (isBn) "💡 গ্রামার টিপস: 'Yesterday' অতীত কাল, তাই verb-এর Past রূপ (went, ate, saw, came) ব্যবহার করুন।"
                else "💡 Grammar tip: With 'yesterday', use the past tense verb (went, ate, saw, came)."

            else -> null
        }
    }

    private fun buildCommonExpressionTip(lower: String, isBn: Boolean): VocabPhrase? = when {
        lower.contains("not sure if you") -> VocabPhrase(
            targetText = "Not sure if you heard / know / are free...",
            pronunciation = if (isBn) "দৈনন্দিন কথোপকথনে ব্যবহার" else "Everyday conversational pattern",
            nativeMeaning = if (isBn) "কাউকে ভদ্রভাবে ও সহজ ভঙ্গিতে কোনো খবর জানাতে বা কিছু জিজ্ঞেস করতে বাক্যের শুরুতে 'Not sure if you...' বলা হয় (যেমন: Not sure if you're free today = জানি না আজ আপনি ফ্রি আছেন কিনা)।"
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
        lower.contains("food") || lower.contains("eat") || lower.contains("cook") || lower.contains("biryani") || lower.contains("coffee") || lower.contains("tea") ->
            "That sounds delicious! Do you usually cook it yourself or enjoy eating out with friends?"
        lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("interview") || lower.contains("developer") ->
            "That sounds like meaningful work! What part of your job do you enjoy the most?"
        lower.contains("travel") || lower.contains("trip") || lower.contains("visit") || lower.contains("country") || lower.contains("airport") ->
            "Traveling is so exciting! Which place would you love to visit next?"
        lower.contains("learn") || lower.contains("english") || lower.contains("study") || lower.contains("practice") ->
            "You're doing a great job practicing! Which part of English do you want to master first — speaking, vocabulary, or grammar?"
        lower.contains("family") || lower.contains("friend") || lower.contains("home") ->
            "Spending time with people we care about is wonderful. What do you usually like doing together?"
        else -> when (turnCount % 3) {
            0 -> "That's really interesting! Could you tell me a little more about it?"
            1 -> "I see what you mean! How do you usually feel about that?"
            else -> "Great point! What happened next, or what are your plans around it?"
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
        val tokens = englishText.lowercase()
            .split(Regex("[^a-zA-Z']+"))
            .filter { it.length >= 3 }
        if (tokens.size == 1) return tokens.first()
        return tokens.firstOrNull { it !in stopWords } ?: tokens.maxByOrNull { it.length }
    }

    private fun isPredominantlyNativeScript(text: String, nativeCode: String, targetCode: String): Boolean {
        if (nativeCode == targetCode) return false
        val bengaliChars = text.count { it in '\u0980'..'\u09FF' }
        val nonAsciiLetters = text.count { it.isLetter() && it.code > 127 }
        val asciiLetters = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        return if (nativeCode == "bn") {
            bengaliChars > 0 && bengaliChars >= asciiLetters
        } else {
            nonAsciiLetters > asciiLetters && targetCode == "en"
        }
    }

    private fun looksLikeQuizAnswer(input: String, question: QuizQuestion): Boolean {
        val trimmed = input.trim()
        val upper = trimmed.uppercase()
        if (upper in setOf("A", "B", "C") || upper.startsWith("A)") || upper.startsWith("B)") || upper.startsWith("C)")) {
            return true
        }
        return question.options.any { opt ->
            opt.equals(trimmed, ignoreCase = true) ||
                opt.substringAfter(") ").equals(trimmed, ignoreCase = true)
        }
    }

    private fun parseQuizOptionIndex(input: String, question: QuizQuestion): Int {
        val trimmed = input.trim()
        val upper = trimmed.uppercase()
        if (upper == "A" || upper.startsWith("A)")) return 0
        if (upper == "B" || upper.startsWith("B)")) return 1
        if (upper == "C" || upper.startsWith("C)")) return 2
        return question.options.indexOfFirst { opt ->
            opt.equals(trimmed, ignoreCase = true) ||
                opt.substringAfter(") ").equals(trimmed, ignoreCase = true)
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
