package com.example.data

import com.example.model.InspireSuggestion
import com.example.model.LanguageLevel
import com.example.model.RolePlayScenario
import com.example.model.TutorMode
import com.example.model.TutorResponse
import com.example.model.UserSettings
import com.example.model.VocabPhrase
import kotlinx.coroutines.delay

/**
 * Unified AI tutor interface. Backed by [PlaceholderAiTutorRepository] when Firebase AI Logic
 * is not enabled, and designed so a live Firebase AI Logic Gemini implementation can be plugged in
 * seamlessly without changing ViewModel or UI layers.
 */
interface AiTutorRepository {
    val isPlaceholderMode: Boolean

    suspend fun generateLessonIntro(
        settings: UserSettings,
        mode: TutorMode,
        topic: String
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
        turnCount: Int
    ): TutorResponse

    suspend fun generateInspireHints(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        lastAiMessage: String?
    ): List<InspireSuggestion>
}

/**
 * Sample / placeholder AI tutor implementation used when Firebase AI Logic is not provisioned.
 * Delivers structured Beginner (A1) to Advanced (C1) bilingual lessons with clean Target–Native
 * bulleted explanations (matching the Bengali + Target language teaching format) for all world languages.
 */
class PlaceholderAiTutorRepository : AiTutorRepository {

    override val isPlaceholderMode: Boolean = true

    override suspend fun generateLessonIntro(
        settings: UserSettings,
        mode: TutorMode,
        topic: String
    ): TutorResponse {
        delay(420)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val level = settings.level
        val isBn = native.code == "bn"

        val phrases = progressiveLessonPhrases(target.code, native.code, level, topic)

        val openerNative = if (settings.studyInNativeLanguage) {
            when {
                isBn && mode == TutorMode.LECTURE ->
                    "ঠিক আছে, চলুন শুরু করি। আজ আমরা ${target.nativeName} (${target.englishName}) ভাষার ${level.bnLabel} (${level.cefr}) স্তরের '$topic' নিয়ে দরকারি বাক্যগুলো সুন্দরভাবে শিখব।"
                isBn && mode == TutorMode.PRACTICE ->
                    "ধন্যবাদ! চলুন এখন একটু অনুশীলন করি। আজকের ${level.bnLabel} (${level.cefr}) প্র্যাকটিসের বিষয় হলো '$topic'।"
                else ->
                    "Alright, let's begin! Today we're learning ${level.label} (${level.cefr}) ${target.englishName} expressions for $topic, explained in ${native.nativeName}."
            }
        } else {
            "Alright, let's begin our ${level.label} (${level.cefr}) ${target.englishName} lesson on $topic."
        }

        val spokenHeadline = if (settings.studyInNativeLanguage && isBn) {
            if (mode == TutorMode.LECTURE) "ঠিক আছে, চলুন শুরু করি।"
            else "ধন্যবাদ! তাহলে চলুন একটু অনুশীলন করি।"
        } else {
            buildTargetGreeting(target.code, level)
        }

        val followUpPrompt = if (settings.studyInNativeLanguage) {
            when {
                isBn && mode == TutorMode.LECTURE ->
                    "এখানে কিছু জানতে চান? কিছু জিজ্ঞেস করতে চান? অথবা নিচের Speak বাটনে ট্যাপ করে প্রথম বাক্যটি বলার চেষ্টা করুন!"
                isBn && mode == TutorMode.PRACTICE ->
                    "এখন আপনার পালা! উপরের যেকোনো একটি বাক্য ব্যবহার করে ${target.englishName}-এ উত্তর দিন — নিচের Speak অথবা Type বাটন ব্যবহার করুন।"
                else ->
                    "Would you like to ask anything here? Tap Play to listen again, or tap Speak to practice aloud!"
            }
        } else {
            "Your turn! Try saying one of these phrases aloud using Speak, or tap Inspire for ideas."
        }

        return TutorResponse(
            spokenText = spokenHeadline,
            nativeExplanation = openerNative,
            phrases = phrases,
            practicePrompt = followUpPrompt,
            grammarFeedback = null,
            isSampleResponse = true
        )
    }

    override suspend fun generateRolePlayIntro(
        settings: UserSettings,
        scenario: RolePlayScenario
    ): TutorResponse {
        delay(450)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"

        val spokenText = rolePlayOpeningLine(target.code, scenario.id)
        val phrases = rolePlayStarterPhrases(target.code, native.code, scenario.id)

        val nativeExplanation = if (settings.studyInNativeLanguage) {
            if (isBn) {
                "ঠিক আছে, চলুন রোল-প্লে শুরু করি! (${scenario.title}) আমি এখন আপনার ${scenario.aiRoleTitle}। নিচের বাক্যগুলোর অর্থ দেখে ${target.englishName}-এ উত্তর দিন।"
            } else {
                "Let's start our ${scenario.title} role-play! I'm your ${scenario.aiRoleTitle}. Use the phrases below to reply in ${target.englishName}."
            }
        } else {
            "Welcome to ${scenario.title}! Goal: ${scenario.mission}"
        }

        val followUp = if (settings.studyInNativeLanguage && isBn) {
            "আপনি এখন কী বলবেন? নিচের Speak বাটনে ট্যাপ করে বলুন অথবা Inspire থেকে আইডিয়া নিন!"
        } else {
            "Respond in character in ${target.englishName}!"
        }

        return TutorResponse(
            spokenText = spokenText,
            nativeExplanation = nativeExplanation,
            phrases = phrases,
            practicePrompt = followUp,
            grammarFeedback = null,
            isSampleResponse = true
        )
    }

    override suspend fun sendMessage(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        userText: String,
        turnCount: Int
    ): TutorResponse {
        delay(520)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val isBn = native.code == "bn"
        val trimmed = userText.trim()
        val wordCount = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        val feedback = buildGentleFeedback(
            userText = trimmed,
            wordCount = wordCount,
            isBn = isBn,
            targetName = target.englishName,
            studyInNative = settings.studyInNativeLanguage,
            level = settings.level
        )

        val spokenReply = if (scenario != null) {
            rolePlayFollowUp(target.code, scenario.id, turnCount)
        } else {
            lessonFollowUp(target.code, mode, settings.level, turnCount)
        }

        val topicHint = scenario?.category ?: settings.interests.firstOrNull() ?: "Daily Life"
        val allPhrases = progressiveLessonPhrases(target.code, native.code, settings.level, topicHint)
        val offset = (turnCount * 2) % allPhrases.size
        val nextPhrases = (0 until minOf(3, allPhrases.size)).map { idx ->
            allPhrases[(offset + idx) % allPhrases.size]
        }

        val nativeExpl = if (settings.studyInNativeLanguage) {
            if (isBn) {
                "ধন্যবাদ! আপনি বলেছেন: \"$trimmed\"। তাহলে চলুন পরের ধাপে আরও সুন্দরভাবে অনুশীলন করি — নিচের বাক্যগুলো ও বাংলা অর্থ দেখুন:"
            } else {
                "Great effort! You said \"$trimmed\". Let's practice the next set of useful ${target.englishName} expressions:"
            }
        } else {
            "Nice reply! Let's build on that with these next ${settings.level.label} expressions:"
        }

        val prompt = if (settings.studyInNativeLanguage && isBn) {
            if (scenario != null) "দারুণ হচ্ছে! রোল-প্লে চালিয়ে যেতে নিচের একটি বাক্য দিয়ে উত্তর দিন।"
            else "এখানে কিছু জানতে চান? নাকি উপরের যেকোনো একটি বাক্য বলার অনুশীলন করবেন?"
        } else {
            if (scenario != null) "Keep the ${scenario.title} conversation going!"
            else "Try repeating one phrase aloud or ask me anything about it!"
        }

        return TutorResponse(
            spokenText = spokenReply,
            nativeExplanation = nativeExpl,
            phrases = nextPhrases,
            practicePrompt = prompt,
            grammarFeedback = feedback,
            isSampleResponse = true
        )
    }

    override suspend fun generateInspireHints(
        settings: UserSettings,
        mode: TutorMode,
        scenario: RolePlayScenario?,
        lastAiMessage: String?
    ): List<InspireSuggestion> {
        delay(180)
        val targetCode = settings.targetLanguageCode
        val isBn = settings.nativeLanguageCode == "bn"
        if (scenario != null) {
            return rolePlayInspireHints(targetCode, isBn, scenario.id)
        }
        val levelPhrases = progressiveLessonPhrases(targetCode, settings.nativeLanguageCode, settings.level, "Daily Life")
        return levelPhrases.take(3).map { phrase ->
            InspireSuggestion(
                targetReply = phrase.targetText,
                nativeHint = phrase.nativeMeaning
            )
        }
    }

    private fun progressiveLessonPhrases(
        targetCode: String,
        nativeCode: String,
        level: LanguageLevel,
        topic: String
    ): List<VocabPhrase> {
        val isBn = nativeCode == "bn"
        return when (level) {
            LanguageLevel.BEGINNER -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "Hello", "Hola", "Bonjour", "Hallo", "مرحباً", "こんにちは", "안녕하세요", "नमस्ते", "你好", "Ciao", "Olá", "হ্যালো", "Merhaba", "Привет"),
                    pronunciation = p(targetCode, "হ্যালো · /həˈloʊ/", "ওলা", "বঁজুর", "হালো", "মারহাবান", "কোন্নিচিওয়া", "আন্নিয়ংহাসিও"),
                    nativeMeaning = if (isBn) "হ্যালো বলার জন্য।" else "Used to say hello politely."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Good morning", "Buenos días", "Bonjour", "Guten Morgen", "صباح الخير", "おはようございます", "좋은 아침이에요", "सुप्रभात", "早上好", "Buongiorno", "Bom dia", "সুপ্রভাত", "Günaydın", "Доброе утро"),
                    pronunciation = p(targetCode, "গুড মর্নিং · /ɡʊd ˈmɔːrnɪŋ/", "বুয়েনোস দিয়াস", "বঁজুর", "গুটেন মর্গেন", "সাবাহ আল-খাইর", "ওহাইয়ো গোজাইমাসু", "জোউন আছিমিয়েয়ো"),
                    nativeMeaning = if (isBn) "সকালে শুভেচ্ছা জানাতে।" else "To greet someone in the morning."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Goodbye", "Adiós", "Au revoir", "Auf Wiedersehen", "مع السلامة", "さようなら", "안녕히 가세요", "अलविदा", "再见", "Arrivederci", "Tchau", "বিদায়", "Hoşça kal", "До свидания"),
                    pronunciation = p(targetCode, "গুডবাই · /ɡʊdˈbaɪ/", "আদিওস", "ও রভোয়া", "আউফ ভিদারজেয়েন", "মা'আস সালামাহ", "সায়োনারা", "আন্নিয়ংহি গাসেয়ো"),
                    nativeMeaning = if (isBn) "বিদায় জানাতে।" else "Used when saying goodbye."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "What's your name?", "¿Cómo te llamas?", "Comment vous appelez-vous ?", "Wie heißen Sie?", "ما اسمك؟", "お名前は何ですか？", "이름이 뭐예요?", "आपका नाम क्या है?", "你叫什么名字？", "Come ti chiami?", "Qual é o seu nome?", "আপনার নাম কী?", "Adınız nedir?", "Как вас зовут?"),
                    pronunciation = p(targetCode, "হোয়াটস ইয়োর নেম? · /wʌts jɔːr neɪm/", "কোমো তে ইয়ামাস", "কমঁ ভুজাপলে ভু", "ভি হাইসেন জি", "মা ইসমুক?", "ওনামায়ে ওয়া নান দেস্কা?", "ইরুমি মুওইয়েয়ো?"),
                    nativeMeaning = if (isBn) "কারো নাম জানতে চাইলে।" else "To ask someone's name."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "My name is...", "Me llamo...", "Je m'appelle...", "Ich heiße...", "اسمي...", "私の名前は...です", "제 이름은...입니다", "मेरा नाम ... है", "我叫...", "Mi chiamo...", "Meu nome é...", "আমার নাম...", "Benim adım...", "Меня зовут..."),
                    pronunciation = p(targetCode, "মাই নেম ইজ... · /maɪ neɪm ɪz/", "মে ইয়ামো", "জ্য মাপেল", "ইশ হাইসে", "ইসমি...", "ওয়াতাশি নো নামায়ে ওয়া...", "জে ইরুমুন..."),
                    nativeMeaning = if (isBn) "নিজের নাম বলতে।" else "To introduce your own name."
                )
            )

            LanguageLevel.ELEMENTARY -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "How are you doing today?", "¿Cómo estás hoy?", "Comment allez-vous aujourd'hui ?", "Wie geht es Ihnen heute?", "كيف حالك اليوم؟", "今日の調子はいかがですか？", "오늘 기분이 어떠세요?", "आज आप कैसे हैं?", "你今天怎么样？", "Come stai oggi?", "Como você está hoje?", "আজ আপনি কেমন আছেন?", "Bugün nasılsınız?", "Как ваши дела сегодня?"),
                    pronunciation = p(targetCode, "হাউ আর ইউ ডুইং টুডে?", "কোমো এস্তাস ওই?", "কমঁ তালে ভু ওজুরদুই?", "ভি গেট এস ইনেন হোয়টে?", "কাইফা হালুকা আল-ইয়াওম?", "কিও নো চোশি ওয়া ইকাগা দেস্কা?", "ওনুল গিবুনি অত্তেসেয়ো?"),
                    nativeMeaning = if (isBn) "আজ কারো খোঁজখবর জানতে চাইলে।" else "Warm way to ask how someone is."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Could you please speak slowly?", "¿Puede hablar despacio, por favor?", "Pouvez-vous parler lentement ?", "Können Sie bitte langsam sprechen?", "هل يمكنك التحدث ببطء؟", "ゆっくり話していただけますか？", "천천히 말씀해 주시겠어요?", "कृपया धीरे बोलें।", "请说慢一点好吗？", "Può parlare lentamente, per favore?", "Pode falar devagar, por favor?", "দয়া করে একটু ধীরে বলবেন?", "Lütfen yavaş konuşabilir misiniz?", "Не могли бы вы говорить медленнее?"),
                    pronunciation = p(targetCode, "কুড ইউ প্লিজ স্পিক স্লোলি?", "পুয়েদে আবলার দেসপাসিও", "পুভে ভু পার্লে লঁতমঁ", "কনেন জি বিটে লাংজাম স্প্রেশেন", "হাল ইউমকিনুকা...", "ইউক্কুরি হানাশিতে...", "ছনছনহি মালসুমহে জুসিগেস্সয়ো?"),
                    nativeMeaning = if (isBn) "কাউকে ধীরে কথা বলার অনুরোধ করতে।" else "Politely ask someone to slow down."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "I would like a cup of tea, please.", "Quisiera una taza de té, por favor.", "Je voudrais une tasse de thé.", "Ich hätte gern eine Tasse Tee.", "أريد كوباً من الشاي من فضلك.", "紅茶を一杯いただけますか。", "차 한 잔 주세요.", "मुझे एक कप चाय चाहिए।", "请给我一杯茶。", "Vorrei una tazza di tè.", "Eu gostaria de uma xícara de chá.", "আমি এক কাপ চা নিতে চাই।", "Bir fincan çay alabilir miyim?", "Я бы хотел чашку чая."),
                    pronunciation = p(targetCode, "আই উড লাইক আ কাপ অব টি, প্লিজ।", "কিসিয়েরা উনা তাসা দে তে", "জ্য ভুদ্রে উন তাস দ্য তে", "ইশ হেটে গার্ন আইনে তাসে টে", "উরিদু কুবান মিন আশ-শাই", "কোচা ও ইপ্পাই ইতাদাকেমাস্কা", "ছা হান জান জুসেয়ো"),
                    nativeMeaning = if (isBn) "ভদ্রভাবে চা বা খাবার অর্ডার করতে।" else "Politely order a drink or meal."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Excuse me, where is the station?", "Disculpe, ¿dónde está la estación?", "Excusez-moi, où est la gare ?", "Entschuldigung, wo ist der Bahnhof?", "عفواً، أين المحطة؟", "すみません、駅はどこですか？", "실례합니다, 역이 어디예요?", "माफ़ कीजिए, स्टेशन कहाँ है?", "请问车站在哪里？", "Mi scusi, dov'è la stazione?", "Com licença, onde fica a estação?", "মাফ করবেন, স্টেশনটি কোথায়?", "Affedersiniz, istasyon nerede?", "Извините, где вокзал?"),
                    pronunciation = p(targetCode, "এক্সকিউজ মি, হোয়্যার ইজ দ্য স্টেশন?", "দিসকুলপে, দোন্দে এস্তা লা এস্তাসিয়ন?", "এক্সকিউজে মোয়া, উ এ লা গার?", "এন্টশুলডিগুং...", "আফওয়ান, আইনা আল-মাহাত্তা?", "সুমিমাসেন, একি ওয়া দোকো দেস্কা?", "শিলিয়েহামনিদা, ইয়গি অদিয়েয়ো?"),
                    nativeMeaning = if (isBn) "রাস্তায় দিকনির্দেশনা জানতে চাইলে।" else "Politely ask for directions."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Thank you so much for your help!", "¡Muchas gracias por tu ayuda!", "Merci beaucoup pour votre aide !", "Vielen Dank für Ihre Hilfe!", "شكراً جزيلاً لمساعدتك!", "助けていただきありがとうございます！", "도와주셔서 정말 감사합니다!", "आपकी मदद के लिए बहुत धन्यवाद!", "非常感谢你的帮助！", "Grazie mille per il tuo aiuto!", "Muito obrigado pela sua ajuda!", "আপনার সাহায্যের জন্য অসংখ্য ধন্যবাদ!", "Yardımınız için çok teşekkürler!", "Большое спасибо за помощь!"),
                    pronunciation = p(targetCode, "থ্যাংক ইউ সো মাচ ফর ইয়োর হেল্প!", "মুচাস গ্রাসিয়াস পোর তু আইউদা!", "মের্সি বোকু পুর ভোত্র এদ!", "ফিলেন ডাংক ফিউর ইরে হিলফে!", "শুকরান জাজিলান...", "তাসুকেতে ইতাদাকি আরিগাতো...", "দোয়াজুশিয়সো জংমাল গামসাহামনিদা!"),
                    nativeMeaning = if (isBn) "কাউকে আন্তরিক ধন্যবাদ জানাতে।" else "Express warm gratitude for help."
                )
            )

            LanguageLevel.INTERMEDIATE -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "In my opinion, $topic is really exciting.", "En mi opinión, $topic es muy emocionante.", "À mon avis, $topic est passionnant.", "Meiner Meinung nach ist $topic spannend.", "في رأيي، هذا الموضوع مثير جداً.", "私の意見では、これはとても面白いです。", "제 생각에는 이 주제가 정말 흥미로워요.", "मेरी राय में यह बहुत रोचक है।", "在我看来，这非常有趣。", "Secondo me è davvero interessante.", "Na minha opinião, isso é empolgante.", "আমার মতে, '$topic' দারুণ আকর্ষণীয়।", "Bence bu gerçekten heyecan verici.", "На мой взгляд, это очень увлекательно."),
                    pronunciation = p(targetCode, "ইন মাই অপিনিয়ন...", "এন মি অপিনিয়ন...", "আ মঁ নাভি...", "মাইনার মাইনুং নাখ...", "ফি রা'য়ি...", "ওয়াতাশি নো ইকেন দেওয়া...", "জে সেংগ্যাগেনুন..."),
                    nativeMeaning = if (isBn) "নিজের মতামত সুন্দরভাবে প্রকাশ করতে।" else "Share your personal opinion clearly."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "I've been practicing every day to improve.", "He estado practicando cada día para mejorar.", "Je m'entraîne chaque jour pour progresser.", "Ich übe jeden Tag, um besser zu werden.", "أتدرب كل يوم لأتحسن.", "上達するために毎日練習しています。", "실력을 늘리기 위해 매일 연습하고 있어요.", "मैं बेहतर होने के लिए रोज़ अभ्यास कर रहा हूँ।", "为了进步，我每天都在练习。", "Mi alleno ogni giorno per migliorare.", "Tenho praticado todos os dias para melhorar.", "উন্নতি করতে আমি প্রতিদিন অনুশীলন করছি।", "Gelişmek için her gün pratik yapıyorum.", "Я практикуюсь каждый день, чтобы стать лучше."),
                    pronunciation = p(targetCode, "আইভ বিন প্র্যাকটিসিং এভরি ডে টু ইমপ্রুভ।", "এ এস্তাদো প্রাকতিকান্দো...", "জ্য মঁত্রেন শাক জুর...", "ইশ ইউবে ইয়েডেন টাগ...", "আতাদাররাবু কুল্লা ইয়াওম...", "জোতাতসু সুরু তামেনি...", "মাইল ইয়নসুপহাগো ইস্সয়ো"),
                    nativeMeaning = if (isBn) "নিয়মিত কোনো অভ্যাস বা কাজের কথা বলতে।" else "Talk about an ongoing daily habit."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Could you explain that in another way?", "¿Podrías explicarlo de otra manera?", "Pourriez-vous l'expliquer autrement ?", "Könnten Sie das anders erklären?", "هل يمكنك شرح ذلك بطريقة أخرى؟", "別の言い方で説明していただけますか？", "다른 방식으로 설명해 주실 수 있나요?", "क्या आप इसे दूसरे तरीके से समझा सकते हैं?", "你能换一种方式解释吗？", "Potresti spiegarlo in un altro modo?", "Você poderia explicar de outra forma?", "এটা কি একটু অন্যভাবে বুঝিয়ে বলবেন?", "Bunu başka bir şekilde açıklayabilir misiniz?", "Не могли бы вы объяснить это иначе?"),
                    pronunciation = p(targetCode, "কুড ইউ এক্সপ্লেইন দ্যাট ইন অ্যানাদার ওয়ে?", "পোদরিয়াস এক্সপ্লিকার্লো...", "পুরিয়ে ভু লেক্সপ্লিকে...", "কনটেন জি ডাস আন্দার্স...", "হাল ইউমকিনুকা শারহু...", "বেৎসু নো ইকাতাদে...", "দারুন বাংসিগুরো..."),
                    nativeMeaning = if (isBn) "কোনো বিষয় আরও পরিষ্কারভাবে বুঝতে চাইলে।" else "Ask for clarification in conversation."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "I'm really looking forward to it!", "¡Tengo muchas ganas de que llegue!", "J'ai vraiment hâte !", "Ich freue mich schon sehr darauf!", "أتطلع إلى ذلك بشوق!", "とても楽しみにしています！", "정말 기대돼요!", "मैं इसके लिए बहुत उत्साहित हूँ!", "我非常期待！", "Non vedo l'ora!", "Estou muito ansioso por isso!", "আমি এটার জন্য অধীর আগ্রহে অপেক্ষা করছি!", "Bunu dört gözle bekliyorum!", "Я очень этого жду!"),
                    pronunciation = p(targetCode, "আইম রিয়েলি লুকিং ফরওয়ার্ড টু ইট!", "তেংগো মুচাস গানাস!", "জে ভ্রেমঁ আত!", "ইশ ফ্রয়ে মিশ...", "আতাতাল্লাউ ইলা জালিকা...", "তোতেমো তানোশিমিনি...", "জংমাল গিদেদুয়েয়ো!"),
                    nativeMeaning = if (isBn) "ভবিষ্যতের কোনো বিষয়ের প্রতি আগ্রহ প্রকাশ করতে।" else "Express excitement about upcoming plans."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "That sounds like a wonderful plan!", "¡Suena como un plan maravilloso!", "Cela semble être un excellent plan !", "Das klingt nach einem tollen Plan!", "تبدو خطة رائعة!", "素晴らしい計画ですね！", "정말 멋진 계획이네요!", "यह एक शानदार योजना लगती है!", "这听起来是个好计划！", "Sembra un piano fantastico!", "Parece um plano maravilhoso!", "এটা তো দারুণ একটি পরিকল্পনা!", "Harika bir plan gibi görünüyor!", "Звучит как отличный план!"),
                    pronunciation = p(targetCode, "দ্যাট সাউন্ডস লাইক আ ওয়ান্ডারফুল প্ল্যান!", "সুয়েনা কোমো উন প্লান...", "স্লা সঁবল এত্র...", "ডাস ক্লিংট...", "তাবদু খুত্তাহ রায়েআহ!", "সুবারাশি কেইকাকু দেস্নে!", "জংমাল মৎজিন গ্যেহোয়েক!"),
                    nativeMeaning = if (isBn) "কারো প্রস্তাব বা পরিকল্পনার প্রশংসা করতে।" else "Agree warmly with someone's suggestion."
                )
            )

            LanguageLevel.UPPER_INTERMEDIATE -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "To be completely honest, it makes a huge difference.", "Para ser sincero, marca una gran diferencia.", "Pour être honnête, cela fait une grande différence.", "Ehrlich gesagt macht das einen großen Unterschied.", "بصراحة تامة، هذا يصنع فرقاً كبيراً.", "正直なところ、大きな違いが生まれます。", "솔직히 말해서 큰 차이가 생겨요.", "सच कहूँ तो इससे बड़ा फर्क पड़ता है।", "老实说，这会产生很大影响。", "Ad essere sinceri, fa una grande differenza.", "Para ser sincero, faz uma enorme diferença.", "সত্যি বলতে, এটি বিশাল পার্থক্য তৈরি করে।", "Dürüst olmak gerekirse büyük fark yaratıyor.", "Честно говоря, это имеет огромное значение."),
                    pronunciation = p(targetCode, "টু বি কমপ্লিটলি অনেস্ট, ইট মেকস আ হিউজ ডিফারেন্স।", "পারা সের সিনসেরো...", "পুর এত্র ওনেত...", "এরলিশ গেজাগট...", "বিসারাহাহ তাম্মাহ...", "শোজিকি না তোকোরো...", "সোলজিকি মালহেসো..."),
                    nativeMeaning = if (isBn) "আত্মবিশ্বাসের সাথে বাস্তব অভিজ্ঞতা জানাতে।" else "Introduce a candid, thoughtful observation."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Let's weigh the pros and cons carefully.", "Evaluemos los pros y los contras con cuidado.", "Pesons soigneusement le pour et le contre.", "Lassen Sie uns Vor- und Nachteile abwägen.", "دعنا نوازن بين الإيجابيات والسلبيات.", "メリットとデメリットを慎重に比べましょう。", "장단점을 신중하게 따져 봅시다.", "आइए फायदे और नुकसान को ध्यान से तौलें।", "让我们仔细权衡利弊。", "Valutiamo attentamente i pro e i contro.", "Vamos pesar os prós e os contras com cuidado.", "চলুন ভালো-মন্দ দিকগুলো ভালোভাবে যাচাই করি।", "Artılarını ve eksilerini dikkatlice değerlendirelim.", "Давайте тщательно взвесим все за и против."),
                    pronunciation = p(targetCode, "লেটস ওয়ে দ্য প্রোস অ্যান্ড কনস কেয়ারফুলি।", "এভালুয়েমোস লোস প্রোস...", "প্যজঁ ল্য পুর এ ল্য কঁত্র", "লাসেন জি উন্স...", "দা'না নুওয়াজিন...", "মেরিত্তো তো দেমেরিত্তো...", "জাংদানজমুল শিনজুংহাগে..."),
                    nativeMeaning = if (isBn) "কোনো সিদ্ধান্ত নেওয়ার আগে সুবিধা-অসুবিধা বিচার করতে।" else "Discuss trade-offs before deciding."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "I couldn't agree with you more.", "No podría estar más de acuerdo contigo.", "Je ne pourrais pas être plus d'accord.", "Da kann ich Ihnen nur voll zustimmen.", "أتفق معك تماماً.", "これ以上ないほど賛成です。", "전적으로 동의합니다.", "मैं आपसे पूरी तरह सहमत हूँ।", "我完全同意你的看法。", "Non potrei essere più d'accordo.", "Não poderia concordar mais com você.", "আমি আপনার সাথে শতভাগ একমত।", "Size kesinlikle katılıyorum.", "Я полностью с вами согласен."),
                    pronunciation = p(targetCode, "আই কুডন্ট অ্যাগ্রি উইথ ইউ মোর।", "নো পোদরিয়া এস্তার মাস...", "জ্য ন পুরে পা এত্র...", "ডা কান ইশ...", "আত্তাফিকু মা'আকা তামামান", "কোরে ইজো নাই...", "জনজগুরো দোংউইহামনিদা"),
                    nativeMeaning = if (isBn) "কারো কথার সাথে জোরালোভাবে একমত হতে।" else "Express strong, natural agreement."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "What really caught my attention was...", "Lo que realmente llamó mi atención fue...", "Ce qui a vraiment attiré mon attention, c'est...", "Was meine Aufmerksamkeit geweckt hat, war...", "ما لفت انتباهي حقاً هو...", "特に私の目を引いたのは...", "제 눈길을 끈 것은...", "जिस चीज़ ने मेरा ध्यान खींचा वह थी...", "真正吸引我注意的是...", "Ciò che ha catturato la mia attenzione è...", "O que realmente chamou minha atenção foi...", "যে বিষয়টি আমার দৃষ্টি আকর্ষণ করেছে তা হলো...", "Asıl dikkatimi çeken şey...", "Что действительно привлекло моё внимание..."),
                    pronunciation = p(targetCode, "হোয়াট রিয়েলি কট মাই অ্যাটেনশন ওয়াজ...", "লো কে ইয়ামো মি আতেনসিয়ন...", "স কি আ আতিরে মঁ নাতঁসিয়ঁ...", "ভাস মাইনে আউফমার্কসামকাইট...", "মা লাফাতা ইন্তিবাহি...", "তোকুনি ওয়াতাশি নো...", "জে নুনগিরুল ক্কুন গসুন..."),
                    nativeMeaning = if (isBn) "আলোচনায় কোনো বিশেষ দিক হাইলাইট করতে।" else "Highlight a standout detail smoothly."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Could we circle back to that point later?", "¿Podemos retomar ese punto más tarde?", "Pouvons-nous revenir sur ce point plus tard ?", "Können wir später darauf zurückkommen?", "هل يمكننا العودة لهذه النقطة لاحقاً؟", "後ほどその点に戻りましょうか？", "나중에 그 부분을 다시 이야기할까요?", "क्या हम उस बिंदु पर बाद में लौट सकते हैं?", "我们稍后再回到这一点好吗？", "Possiamo tornare su quel punto più tardi?", "Podemos voltar a esse ponto mais tarde?", "আমরা কি বিষয়টি নিয়ে একটু পরে আবার কথা বলতে পারি?", "Bu konuya daha sonra dönebilir miyiz?", "Можем ли мы вернуться к этому вопросу позже?"),
                    pronunciation = p(targetCode, "কুড উই সার্কল ব্যাক টু দ্যাট পয়েন্ট লেটার?", "পোদেমোস রেতোমার...", "পুভঁ নু রভনির...", "কনেন ভির স্পেটার...", "হাল ইউমকিনুনা আল-আওদাহ...", "নচিহোদো সোনো তেন...", "নাজুংএ গু বুবুনুল..."),
                    nativeMeaning = if (isBn) "মিটিং বা আলাপে কোনো প্রসঙ্গ পরে আলোচনা করার জন্য রাখতে।" else "Smooth professional transition phrase."
                )
            )

            LanguageLevel.ADVANCED -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "At the end of the day, consistency outweighs intensity.", "Al final del día, la constancia supera a la intensidad.", "Au bout du compte, la régularité l'emporte sur l'intensité.", "Letztendlich zählt Beständigkeit mehr als Intensität.", "في نهاية المطاف، الاستمرارية تتفوق على الحماس المؤقت.", "結局のところ、継続は一時の勢いに勝ります。", "결국 꾸준함이 단기적인 강도보다 중요해요.", "आखिरकार, निरंतरता तीव्रता से भारी पड़ती है।", "归根结底，持之以恒胜过一时冲动。", "In fin dei conti, la costanza supera l'intensità.", "No fim das contas, a consistência supera a intensidade.", "দিনশেষে, সাময়িক উচ্ছ্বাসের চেয়ে নিয়মিত ধারাবাহিকতাই বেশি কার্যকর।", "Günün sonunda istikrar yoğunluktan daha önemlidir.", "В конечном счёте постоянство важнее интенсивности."),
                    pronunciation = p(targetCode, "অ্যাট দ্য এন্ড অব দ্য ডে, কনসিস্টেন্সি আউটওয়েজ ইনটেনসিটি।", "আল ফিনাল দেল দিয়া...", "ও বু দু কঁত...", "লেটস্টএন্ডলিশ...", "ফি নিহায়াতিল মাতাফ...", "কেক্কিওকু নো তোকোরো...", "গিয়লগুক ক্কুজুনহামি..."),
                    nativeMeaning = if (isBn) "গভীর ও প্রভাবশালীভাবে মূল সত্য তুলে ধরতে।" else "Articulate a persuasive big-picture insight."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "That raises an intriguing perspective worth exploring.", "Eso plantea una perspectiva fascinante que vale la pena explorar.", "Cela soulève une perspective fascinante à explorer.", "Das eröffnet eine spannende Perspektive.", "هذا يطرح منظوراً مثيراً يستحق الاستكشاف.", "それは探求する価値のある興味深い視点ですね。", "탐구해 볼 가치가 있는 흥미로운 관점이네요.", "यह एक दिलचस्प दृष्टिकोण सामने लाता है।", "这提出了一个值得探讨的有趣视角。", "Questo solleva una prospettiva affascinante.", "Isso traz uma perspectiva fascinante que vale explorar.", "এটি একটি দারুণ ও গভীর দৃষ্টিভঙ্গি তুলে ধরেছে যা নিয়ে ভাবা উচিত।", "Bu, keşfetmeye değer ilginç bir bakış açısı sunuyor.", "Это открывает интересную перспективу."),
                    pronunciation = p(targetCode, "দ্যাট রেইজেস অ্যান ইন্ট্রিগিং পার্সপেক্টিভ...", "এসো প্লান্তেয়া উনা...", "স্লা সুলেভ...", "ডাস এরঅফনেট...", "হাজা ইয়াতরাহু মানজুরান...", "সোরে ওয়া তানকিউ...", "তামগুহে বোল গাছিগা..."),
                    nativeMeaning = if (isBn) "বুদ্ধিদীপ্তভাবে কারো যুক্তির প্রশংসা ও বিশ্লেষণ করতে।" else "Diplomatically acknowledge a nuanced idea."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Let's bridge the gap between strategy and execution.", "Cerremos la brecha entre la estrategia y la ejecución.", "Comblons le fossé entre la stratégie et l'exécution.", "Schließen wir die Lücke zwischen Strategie und Umsetzung.", "لنُغلق الفجوة بين الاستراتيجية والتنفيذ.", "戦略と実行のギャップを埋めましょう。", "전략과 실행 사이의 간극을 좁혀 봅시다.", "आइए रणनीति और क्रियान्वयन के बीच की खाई पाटें।", "让我们弥合战略与执行之间的差距。", "Colmiamo il divario tra strategia ed esecuzione.", "Vamos preencher a lacuna entre estratégia e execução.", "চলুন পরিকল্পনা ও বাস্তব প্রয়োগের মধ্যকার দূরত্ব কমাই।", "Strateji ile uygulama arasındaki boşluğu kapatalım.", "Давайте сократим разрыв между стратегией и реализацией."),
                    pronunciation = p(targetCode, "লেটস ব্রিজ দ্য গ্যাপ বিটুইন স্ট্র্যাটেজি অ্যান্ড এক্সিকিউশন।", "সেরেমোস লা ব্রেচা...", "কঁবলঁ ল্য ফসে...", "শ্লিসেন ভির...", "লিনুগলিক আল-ফাজওয়াহ...", "সেনরিয়াকু তো জিক্কো...", "জনরিয়াকগোয়া শিলহেং..."),
                    nativeMeaning = if (isBn) "উচ্চপর্যায়ের নেতৃত্ব ও পেশাদার আলোচনায় ব্যবহৃত।" else "Executive-level phrasing for alignment."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "I'd truly appreciate your candid feedback on this.", "Agradecería mucho tus comentarios sinceros sobre esto.", "J'apprécierais vraiment votre retour franc à ce sujet.", "Ich wäre für Ihr offenes Feedback sehr dankbar.", "سأقدر حقاً ملاحظاتك الصريحة حول هذا.", "これについて率直なご意見をいただければ幸いです。", "이에 대한 솔직한 피드백을 주시면 감사하겠습니다.", "मैं इस पर आपकी स्पष्ट प्रतिक्रिया की सराहना करूँगा।", "我非常感谢您对此提出坦诚的反馈。", "Apprezzerei davvero il tuo feedback sincero.", "Eu realmente agradeceria seu feedback sincero.", "এই বিষয়ে আপনার খোলামেলা ও সৎ মতামত পেলে আমি সত্যিই কৃতজ্ঞ থাকব।", "Bu konudaki samimi geri bildiriminize çok değer veririm.", "Я буду искренне признателен за ваш честный отзыв."),
                    pronunciation = p(targetCode, "আইড ট্রুলি অ্যাপ্রিশিয়েট ইয়োর ক্যান্ডিড ফিডব্যাক।", "আগ্রাদেসেরিয়া মুচো...", "জাপ্রেসিয়ারে ভ্রেমঁ...", "ইশ ভ্যারে ফিউর...", "সা'উকাদ্দিরু হাক্কান...", "সোচ্চোকু না গোইকেন...", "সোলজিকান ফিদুবেগুল..."),
                    nativeMeaning = if (isBn) "পেশাদার ও মার্জিত ভঙ্গিতে সৎ মতামত চাইতে।" else "Invite honest feedback with poise."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Needless to say, nuance shapes how we connect.", "Sobra decir que los matices moldean nuestra conexión.", "Il va sans dire que la nuance façonne nos liens.", "Es versteht sich von selbst, dass Nuancen verbinden.", "غني عن القول إن الدقة في التعبير تقربنا.", "言うまでもなく、細やかな表現が人をつなぎます。", "말할 필요도 없이 섬세한 뉘앙스가 소통을 만듭니다.", "कहने की ज़रूरत नहीं कि सूक्ष्मता संबंध बनाती है।", "不用说，细微的表达塑造了人与人的联结。", "Inutile dire che le sfumature creano connessione.", "Nem precisa dizer que a nuance molda nossa conexão.", "বলাই বাহুল্য, সূক্ষ্ম প্রকাশভঙ্গিই আমাদের যোগাযোগকে গভীর করে।", "Söylemeye gerek yok, incelik iletişimimizi şekillendirir.", "Само собой разумеется, нюансы формируют общение."),
                    pronunciation = p(targetCode, "নিডলেস টু সে, নুয়ান্স শেপস হাউ উই কানেক্ট।", "সোব্রা দেসির...", "ইল ভা সঁ দির...", "এস ফারস্টেট জিশ...", "গানিউন আনিল কাওল...", "ইউ মাদেমো নাকু...", "মালহাল পিরিওদো অপসি..."),
                    nativeMeaning = if (isBn) "সাবলীল ও উচ্চমার্গীয় কথোপকথনের সূচনা করতে।" else "Polished idiomatic connector for fluent speech."
                )
            )
        }
    }

    private fun buildTargetGreeting(targetCode: String, level: LanguageLevel): String = when (targetCode) {
        "es" -> "¡Hola! Comencemos nuestra lección de nivel ${level.cefr}."
        "fr" -> "Bonjour ! Commençons notre leçon de niveau ${level.cefr}."
        "de" -> "Hallo! Starten wir unsere Lektion für Niveau ${level.cefr}."
        "ja" -> "こんにちは！レベル${level.cefr}のレッスンを始めましょう。"
        "ko" -> "안녕하세요! ${level.cefr} 레벨 레슨을 시작해 볼까요?"
        "ar" -> "مرحباً! لنبدأ درسنا للمستوى ${level.cefr}."
        "hi" -> "नमस्ते! आइए ${level.cefr} स्तर का पाठ शुरू करें।"
        "bn" -> "ঠিক আছে, চলুন শুরু করি।"
        else -> "Alright, let's begin your ${level.label} (${level.cefr}) lesson!"
    }

    private fun buildGentleFeedback(
        userText: String,
        wordCount: Int,
        isBn: Boolean,
        targetName: String,
        studyInNative: Boolean,
        level: LanguageLevel
    ): String {
        return if (studyInNative && isBn) {
            when {
                wordCount <= 2 -> "চমৎকার শুরু! বাক্যটি আরেকটু বড় করে বললে $targetName আরও স্বাভাবিক ও সুন্দর শোনাবে।"
                else -> "অসাধারণ বলেছেন! আপনার বাক্যটি ${level.bnLabel} (${level.cefr}) স্তরের জন্য পরিষ্কার ও প্রাণবন্ত হয়েছে।"
            }
        } else {
            when {
                wordCount <= 2 -> "Great start! Try adding one more detail to make your $targetName sentence flow naturally."
                else -> "Well said! Clear and natural phrasing for ${level.label} (${level.cefr})."
            }
        }
    }

    private fun rolePlayOpeningLine(targetCode: String, scenarioId: String): String = when (scenarioId) {
        "cafe_cooking" -> t(targetCode, "Good morning and welcome! What can I prepare for you today?", "¡Buenos días y bienvenido! ¿Qué le preparo hoy?", "Bonjour et bienvenue ! Que puis-je vous préparer ?", "Guten Morgen! Was darf ich Ihnen heute zubereiten?", "صباح الخير! ماذا أجهز لك اليوم؟", "おはようございます！今日は何をお作りしましょうか？", "안녕하세요! 오늘 무엇을 준비해 드릴까요?", "सुप्रभात! आज मैं आपके लिए क्या तैयार करूँ?", "早上好！今天想点些什么？", "Buongiorno! Cosa posso prepararle oggi?", "Bom dia! O que posso preparar para você hoje?", "সুপ্রভাত! আজ আপনার জন্য কী তৈরি করব?", "Günaydın! Bugün size ne hazırlayabilirim?", "Доброе утро! Что вам приготовить сегодня?")
        "airport_travel" -> t(targetCode, "Hello! Welcome to check-in. May I see your passport and luggage?", "¡Hola! Bienvenido al mostrador. ¿Puedo ver su pasaporte?", "Bonjour ! Bienvenue à l'enregistrement. Puis-je voir votre passeport ?", "Hallo! Darf ich bitte Ihren Reisepass sehen?", "مرحباً! هل يمكنني رؤية جواز سفرك؟", "こんにちは！パスポートを拝見できますか？", "안녕하세요! 여권을 보여주시겠어요?", "नमस्ते! क्या मैं आपका पासपोर्ट देख सकता हूँ?", "您好！请出示您的护照。", "Buongiorno! Posso vedere il suo passaporto?", "Olá! Posso ver seu passaporte?", "স্বাগতম! আপনার পাসপোর্টটি একটু দেখাবেন?", "Merhaba! Pasaportunuzu görebilir miyim?", "Здравствуйте! Покажите, пожалуйста, ваш паспорт.")
        "job_interview" -> t(targetCode, "Thanks for joining me today! Tell me about a project you are proud of.", "¡Gracias por venir hoy! Háblame de un proyecto del que estés orgulloso.", "Merci d'être venu ! Parlez-moi d'un projet dont vous êtes fier.", "Danke, dass Sie heute hier sind! Erzählen Sie mir von Ihrem Projekt.", "شكراً لحضورك اليوم! حدثني عن مشروع تفتخر به.", "本日はありがとうございます！誇りに思うプロジェクトを教えてください。", "오늘 와 주셔서 감사합니다! 자랑스러운 프로젝트에 대해 말해 주세요.", "आज आने के लिए धन्यवाद! अपने किसी प्रोजेक्ट के बारे में बताएं।", "感谢您今天参加面试！请介绍一下您引以为傲的项目。", "Grazie per essere qui oggi! Mi parli di un progetto di cui è fiero.", "Obrigado por vir hoje! Conte-me sobre um projeto de que se orgulha.", "আজ আসার জন্য ধন্যবাদ! আপনার প্রিয় একটি প্রজেক্ট সম্পর্কে বলুন।", "Bugün geldiğiniz için teşekkürler! Gurur duyduğunuz bir projeyi anlatır mısınız?", "Спасибо, что пришли сегодня! Расскажите о проекте, которым вы гордитесь.")
        else -> t(targetCode, "Welcome in! Are you looking for something special today?", "¡Bienvenido! ¿Busca algo especial hoy?", "Bienvenue ! Cherchez-vous quelque chose de spécial ?", "Willkommen! Suchen Sie heute etwas Besonderes?", "أهلاً بك! هل تبحث عن شيء مميز اليوم؟", "いらっしゃいませ！何かお探しですか？", "어서 오세요! 오늘 특별한 것을 찾고 계신가요?", "स्वागत है! क्या आप आज कुछ खास ढूँढ रहे हैं?", "欢迎光临！您今天想找点什么？", "Benvenuto! Cerca qualcosa di speciale oggi?", "Bem-vindo! Está procurando algo especial hoje?", "স্বাগতম! আজ কি বিশেষ কিছু খুঁজছেন?", "Hoş geldiniz! Bugün özel bir şey mi arıyorsunuz?", "Добро пожаловать! Вы ищете что-то особенное?")
    }

    private fun rolePlayFollowUp(targetCode: String, scenarioId: String, turnCount: Int): String = when (scenarioId) {
        "cafe_cooking" -> t(targetCode, "Wonderful choice! Would you like it hot or iced?", "¡Excelente elección! ¿Lo prefiere caliente o frío?", "Excellent choix ! Chaud ou glacé ?", "Wunderbare Wahl! Heiß oder auf Eis?", "اختيار رائع! هل تفضله ساخناً أم بارداً؟", "素晴らしい選択です！ホットとアイスどちらにしますか？", "좋은 선택이에요! 따뜻하게 드릴까요, 차갑게 드릴까요?", "बढ़िया पसंद! गरम या ठंडा?", "很好的选择！您要热的还是冰的？", "Ottima scelta! Caldo o freddo?", "Ótima escolha! Quente ou gelado?", "দারুণ পছন্দ! গরম নাকি ঠান্ডা নেবেন?", "Harika seçim! Sıcak mı soğuk mu istersiniz?", "Отличный выбор! Горячий или со льдом?")
        "airport_travel" -> t(targetCode, "All set! You have window seat 14A at Gate A12.", "¡Listo! Tiene el asiento de ventanilla 14A en la puerta A12.", "C'est parfait ! Siège hublot 14A, porte A12.", "Alles bereit! Fensterplatz 14A an Gate A12.", "تم كل شيء! مقعدك 14A بجانب النافذة عند البوابة A12.", "準備できました！A12ゲートの窓側14A席です。", "완료되었습니다! A12 게이트 창가 14A 좌석입니다.", "हो गया! गेट A12 पर आपकी खिड़की की सीट 14A है।", "办好了！您在A12登机口，靠窗座位14A。", "Tutto pronto! Posto finestrino 14A, gate A12.", "Tudo certo! Assento janela 14A no portão A12.", "সব ঠিক আছে! গেট A12-এ আপনার জানালার সিট 14A।", "Tamamdır! A12 kapısında 14A cam kenarı koltuğunuz hazır.", "Готово! Ваше место у окна 14A, выход A12.")
        "job_interview" -> t(targetCode, "Impressive impact! What questions would you like to ask our team?", "¡Impresionante! ¿Qué preguntas tiene para nuestro equipo?", "Très impressionnant ! Avez-vous des questions pour notre équipe ?", "Beeindruckend! Welche Fragen haben Sie an unser Team?", "رائع جداً! ما الأسئلة التي تود طرحها على فريقنا؟", "素晴らしい成果ですね！チームへの質問はありますか？", "인상적이네요! 우리 팀에 궁금한 점이 있으신가요?", "प्रभावशाली! हमारी टीम के लिए आपके क्या प्रश्न हैं?", "令人印象深刻！您对我们团队有什么问题吗？", "Davvero notevole! Ha domande per il nostro team?", "Impressionante! Que perguntas gostaria de fazer à equipe?", "অসাধারণ! আমাদের টিম সম্পর্কে আপনার কোনো প্রশ্ন আছে কি?", "Etkileyici! Ekibimize sormak istediğiniz sorular var mı?", "Впечатляет! Какие вопросы у вас есть к нашей команде?")
        else -> t(targetCode, "Great eye! Would you like to try it on? We also offer a bundle discount.", "¡Buen gusto! ¿Quiere probárselo? Tenemos descuento por paquete.", "Très bon choix ! Voulez-vous l'essayer ?", "Gutes Auge! Möchten Sie es anprobieren?", "ذوق رائع! هل تود تجربته؟ لدينا خصم خاص.", "お目が高いですね！ご試着されますか？", "안목이 좋으시네요! 입어 보시겠어요?", "अच्छी पसंद है! क्या आप इसे आज़माना चाहेंगे?", "眼光真好！想试穿一下吗？", "Ottimo gusto! Vuole provarlo?", "Bom gosto! Gostaria de experimentar?", "দারুণ পছন্দ! ট্রাই করে দেখতে চান? দুটি নিলে ছাড় আছে।", "Harika seçim! Denemek ister misiniz?", "Отличный вкус! Хотите примерить?")
    }

    private fun lessonFollowUp(targetCode: String, mode: TutorMode, level: LanguageLevel, turnCount: Int): String =
        if (mode == TutorMode.LECTURE) {
            t(targetCode, "Well done! Let's look at the next ${level.label} phrases together.", "¡Muy bien! Veamos las siguientes frases juntos.", "Bravo ! Regardons les phrases suivantes ensemble.", "Sehr gut! Schauen wir uns die nächsten Sätze an.", "أحسنت! لنتعلم العبارات التالية معاً.", "よくできました！次のフレーズを一緒に見てみましょう。", "잘하셨어요! 다음 표현들을 함께 살펴볼까요?", "बहुत बढ़िया! आइए अगले वाक्य साथ में देखें।", "做得好！我们一起来看看下一组表达。", "Ben fatto! Vediamo le prossime frasi insieme.", "Muito bem! Vamos ver as próximas frases juntos.", "দারুণ হয়েছে! চলুন পরের বাক্যগুলো একসাথে দেখি।", "Harika! Sıradaki cümlelere birlikte bakalım.", "Отлично! Давайте разберём следующие фразы вместе.")
        } else {
            if (turnCount % 2 == 0) {
                t(targetCode, "Awesome! Can you try using one of these phrases in a full sentence?", "¡Genial! ¿Puedes usar una de estas frases en una oración completa?", "Super ! Pouvez-vous utiliser une de ces phrases ?", "Super! Kannst du einen dieser Ausdrücke in einem Satz nutzen?", "ممتاز! هل يمكنك استخدام إحدى هذه العبارات في جملة؟", "素晴らしい！この表現を使って一文作ってみましょうか？", "멋져요! 이 표현 중 하나로 문장을 만들어 보시겠어요?", "शानदार! क्या आप इनमें से एक वाक्य का उपयोग कर सकते हैं?", "太棒了！能用其中一个表达造个句子吗？", "Fantastico! Riesci a usare una di queste frasi?", "Ótimo! Consegue usar uma dessas frases?", "চমৎকার! নিচের একটি বাক্য দিয়ে নিজে কথা বলার চেষ্টা করবেন?", "Harika! Bu ifadelerden birini cümle içinde kullanabilir misiniz?", "Отлично! Попробуйте использовать одну из этих фраз в предложении.")
            } else {
                t(targetCode, "You're sounding more natural already! What would you say next?", "¡Ya suenas mucho más natural! ¿Qué dirías después?", "Vous parlez déjà plus naturellement ! Que diriez-vous ensuite ?", "Das klingt schon sehr natürlich! Was würden Sie als Nächstes sagen?", "أصبح نطقك أكثر طبيعية! ماذا ستقول بعد ذلك؟", "もうとても自然ですね！次は何と言いますか？", "벌써 훨씬 자연스러워요! 다음엔 뭐라고 말할까요?", "आपकी भाषा बहुत स्वाभाविक लग रही है! आगे क्या कहेंगे?", "说得越来越自然了！接下来您会怎么说？", "Sembra già molto più naturale! Cosa direbbe dopo?", "Você já soa bem mais natural! O que diria agora?", "আপনার বলাটা অনেক সুন্দর হচ্ছে! এরপর আপনি কী বলবেন?", "Şimdiden çok doğal konuşuyorsunuz! Sırada ne söylersiniz?", "Уже звучит очень естественно! Что бы вы сказали дальше?")
            }
        }

    private fun rolePlayStarterPhrases(targetCode: String, nativeCode: String, scenarioId: String): List<VocabPhrase> {
        val isBn = nativeCode == "bn"
        return when (scenarioId) {
            "cafe_cooking" -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "Could I have an iced latte, please?", "¿Me das un café con leche frío, por favor?", "Je voudrais un latte glacé, s'il vous plaît.", "Ich hätte gern einen Eislatte, bitte.", "هل يمكنني الحصول على لاتيه مثلج؟", "アイスラテをいただけますか？", "아이스 라떼 한 잔 주세요.", "क्या मुझे एक आइस्ड लाते मिल सकती है?", "请给我一杯冰拿铁。", "Posso avere un latte freddo?", "Pode me ver um latte gelado?", "আমাকে একটি আইসড লাতে দেবেন?", "Bir buzlu latte alabilir miyim?", "Можно мне холодный латте?"),
                    pronunciation = p(targetCode, "কুড আই হ্যাভ অ্যান আইসড লাতে, প্লিজ?", "মে দাস উন কাফে...", "জ্য ভুদ্রে উন লাতে গ্লাসে...", "ইশ হেটে গার্ন...", "লাতিহ মুসাল্লাজ", "আইসু রাতে ও...", "আইসু রাত্তে হান জান..."),
                    nativeMeaning = if (isBn) "কফি অর্ডার করার ভদ্র উপায়।" else "Polite way to order coffee."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "What is today's chef special?", "¿Cuál es el plato especial de hoy?", "Quel est le plat du jour ?", "Was ist heute das Tagesgericht?", "ما هو طبق اليوم المميز؟", "今日のスペシャルは何ですか？", "오늘의 추천 메뉴는 무엇인가요?", "आज का स्पेशल क्या है?", "今天的特色菜是什么？", "Qual è il piatto del giorno?", "Qual é o prato especial de hoje?", "আজকের স্পেশাল খাবার কী?", "Günün özel yemeği nedir?", "Какое сегодня блюдо дня?"),
                    pronunciation = p(targetCode, "হোয়াট ইজ টুডে'স শেফ স্পেশাল?", "কুয়াল এস এল প্লাতো...", "কেল এ ল্য প্লা দু জুর?", "ভাস ইস্ত...", "মা হুয়া তাবাক...", "কিও নো সুপেশারু...", "ওনুরে ছুছন মেনু..."),
                    nativeMeaning = if (isBn) "আজকের বিশেষ খাবার সম্পর্কে জানতে।" else "Ask about the daily special."
                )
            )
            "airport_travel" -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "I'd like a window seat, please.", "Quisiera un asiento de ventanilla.", "Je voudrais un siège hublot.", "Ich hätte gern einen Fensterplatz.", "أريد مقعداً بجانب النافذة.", "窓側の席をお願いします。", "창가 자리로 부탁드립니다.", "मुझे खिड़की की सीट चाहिए।", "我想要靠窗的座位。", "Vorrei un posto finestrino.", "Gostaria de um assento na janela.", "আমি জানালার পাশের সিট চাই।", "Cam kenarı koltuk istiyorum.", "Я бы хотел место у окна."),
                    pronunciation = p(targetCode, "আইড লাইক আ উইন্ডো সিট, প্লিজ।", "কিসিয়েরা উন আসিয়েন্তো...", "জ্য ভুদ্রে উন সিয়েজ উবলো", "ইশ হেটে গার্ন...", "মাক'আদান বিজানিব...", "মাদোগাওয়া নো সেকি...", "ছাংগা জারিরো..."),
                    nativeMeaning = if (isBn) "জানালার পাশের সিট চাইতে।" else "Request a window seat."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Which gate is my flight boarding from?", "¿Por qué puerta embarca mi vuelo?", "À quelle porte embarque mon vol ?", "An welchem Gate startet mein Flug?", "من أي بوابة تقلع رحلتي؟", "私の便は何番ゲートですか？", "제 비행기는 몇 번 게이트인가요?", "मेरी फ्लाइट किस गेट से है?", "我的航班在哪个登机口？", "Da quale gate parte il mio volo?", "Qual é o portão do meu voo?", "আমার ফ্লাইট কোন গেট থেকে ছাড়বে?", "Uçuşum hangi kapıdan?", "С какого выхода посадка на мой рейс?"),
                    pronunciation = p(targetCode, "হুইচ গেট ইজ মাই ফ্লাইট বোর্ডিং ফ্রম?", "পোর কে পুয়ের্তা...", "আ কেল পর্ট...", "আন ভেলশেম গেট...", "মিন আই বাউয়াবাহ...", "নানবান গেতো দেস্কা?", "মিয়ত বন গেইতু..."),
                    nativeMeaning = if (isBn) "বোর্ডিং গেট জানতে চাইলে।" else "Confirm your boarding gate."
                )
            )
            else -> progressiveLessonPhrases(targetCode, nativeCode, LanguageLevel.ELEMENTARY, "Daily Life").take(2)
        }
    }

    private fun rolePlayInspireHints(targetCode: String, isBn: Boolean, scenarioId: String): List<InspireSuggestion> {
        return rolePlayStarterPhrases(targetCode, if (isBn) "bn" else "en", scenarioId).map {
            InspireSuggestion(targetReply = it.targetText, nativeHint = it.nativeMeaning)
        } + InspireSuggestion(
            targetReply = t(targetCode, "Thank you very much! That sounds great.", "¡Muchas gracias! Suena genial.", "Merci beaucoup ! C'est parfait.", "Vielen Dank! Das klingt toll.", "شكراً جزيلاً! هذا رائع.", "ありがとうございます！いいですね。", "정말 감사합니다! 좋네요.", "बहुत धन्यवाद! यह बढ़िया है।", "非常感谢！太好了。", "Grazie mille! Perfetto.", "Muito obrigado! Ótimo.", "অনেক ধন্যবাদ! দারুণ হবে।", "Çok teşekkürler! Harika.", "Большое спасибо! Отлично."),
            nativeHint = if (isBn) "ভদ্রভাবে ধন্যবাদ ও সম্মতি জানাতে।" else "Reply warmly and politely."
        )
    }

    private fun t(
        code: String,
        en: String,
        es: String,
        fr: String,
        de: String,
        ar: String,
        ja: String,
        ko: String,
        hi: String,
        zh: String,
        it: String,
        pt: String,
        bn: String,
        tr: String,
        ru: String
    ): String = when (code) {
        "es" -> es
        "fr" -> fr
        "de" -> de
        "ar" -> ar
        "ja" -> ja
        "ko" -> ko
        "hi" -> hi
        "zh" -> zh
        "it" -> it
        "pt" -> pt
        "bn" -> bn
        "tr" -> tr
        "ru" -> ru
        else -> en
    }

    private fun p(
        code: String,
        enPron: String,
        esPron: String,
        frPron: String,
        dePron: String,
        arPron: String,
        jaPron: String,
        koPron: String
    ): String = when (code) {
        "en" -> enPron
        "es" -> esPron
        "fr" -> frPron
        "de" -> dePron
        "ar" -> arPron
        "ja" -> jaPron
        "ko" -> koPron
        else -> ""
    }
}
