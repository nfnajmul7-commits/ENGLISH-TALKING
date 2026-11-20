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
 * Dynamically adapts its sample explanations, vocabulary, and prompts to the user's selected
 * Native Language (including rich Bengali support), Target Language, Language Level, Mode,
 * Interests, and Role-Play scenario.
 */
class PlaceholderAiTutorRepository : AiTutorRepository {

    override val isPlaceholderMode: Boolean = true

    override suspend fun generateLessonIntro(
        settings: UserSettings,
        mode: TutorMode,
        topic: String
    ): TutorResponse {
        delay(550)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val level = settings.level
        val phrases = samplePhrasesFor(target.code, native.code, topic, level)

        val spokenText = when (mode) {
            TutorMode.LECTURE -> buildLectureGreeting(target.code, topic, level)
            TutorMode.PRACTICE -> buildPracticeGreeting(target.code, topic, level)
        }

        val nativeExplanation = if (settings.studyInNativeLanguage) {
            buildNativeExplanation(
                nativeCode = native.code,
                nativeName = native.nativeName,
                targetName = target.englishName,
                topic = topic,
                mode = mode,
                level = level
            )
        } else {
            "[Sample ${level.cefr} ${mode.name.lowercase().replaceFirstChar { it.uppercase() }}] Let's focus on $topic in ${target.englishName}. Listen closely to the rhythm and tap Play Audio to practice with Aria."
        }

        val practicePrompt = when (mode) {
            TutorMode.LECTURE -> "Try repeating the first phrase aloud using Speak, or tap Inspire for ideas!"
            TutorMode.PRACTICE -> "Your turn! Answer Aria's question in ${target.englishName} using your voice or keyboard."
        }

        return TutorResponse(
            spokenText = spokenText,
            nativeExplanation = nativeExplanation,
            phrases = phrases,
            practicePrompt = practicePrompt,
            grammarFeedback = null,
            isSampleResponse = true
        )
    }

    override suspend fun generateRolePlayIntro(
        settings: UserSettings,
        scenario: RolePlayScenario
    ): TutorResponse {
        delay(550)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val spokenText = rolePlayOpeningLine(target.code, scenario.id)
        val phrases = rolePlayStarterPhrases(target.code, native.code, scenario.id)

        val nativeExplanation = if (settings.studyInNativeLanguage) {
            if (native.code == "bn") {
                "নমুনা রোল-প্লে (${scenario.title}): আমি এখন আপনার ${scenario.aiRoleTitle}। আপনার লক্ষ্য: ${scenario.mission} নিচের বাক্যগুলো ব্যবহার করে ${target.englishName}-এ উত্তর দিন।"
            } else {
                "[Sample Role-Play · ${native.nativeName}] Scenario: ${scenario.title}. I'm playing the ${scenario.aiRoleTitle}. Mission: ${scenario.mission}"
            }
        } else {
            "[Sample Role-Play] Welcome to ${scenario.title}! Goal: ${scenario.mission}"
        }

        return TutorResponse(
            spokenText = spokenText,
            nativeExplanation = nativeExplanation,
            phrases = phrases,
            practicePrompt = "Respond in character as yourself in ${target.englishName}!",
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
        delay(650)
        val target = settings.targetLanguage
        val native = settings.nativeLanguage
        val trimmed = userText.trim()
        val wordCount = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        val feedback = buildGentleFeedback(
            userText = trimmed,
            wordCount = wordCount,
            nativeCode = native.code,
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
        val phrases = samplePhrasesFor(target.code, native.code, topicHint, settings.level)
            .drop(turnCount % 2)
            .take(2)

        val nativeExpl = if (settings.studyInNativeLanguage) {
            if (native.code == "bn") {
                "দারুণ চেষ্টা! [নমুনা উত্তর] আপনি বলেছেন: \"$trimmed\"। কথোপকথন এগিয়ে নিতে নিচের সহজ অভিব্যক্তিগুলো চেষ্টা করুন।"
            } else {
                "[Sample Tutor · ${native.nativeName}] Great effort! You said \"$trimmed\". Try weaving one of the highlighted ${target.englishName} expressions into your next reply."
            }
        } else {
            "[Sample Tutor] Nice reply! Keep building fluency by adding one extra detail to your next sentence."
        }

        val prompt = if (scenario != null) {
            "Continue the ${scenario.title} role-play — ask a follow-up question!"
        } else if (mode == TutorMode.LECTURE) {
            "Tap Play Audio to shadow the pronunciation, then try forming your own example."
        } else {
            "Keep the conversation flowing! Answer Aria's question aloud."
        }

        return TutorResponse(
            spokenText = spokenReply,
            nativeExplanation = nativeExpl,
            phrases = phrases,
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
        delay(220)
        val targetCode = settings.targetLanguageCode
        val isBn = settings.nativeLanguageCode == "bn"
        return when (scenario?.id) {
            "cafe_cooking" -> listOf(
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "Could I have an iced oat latte and a fresh croissant, please?", "¿Me das un café con leche de avena y un croissant fresco, por favor?", "Je voudrais un latte glacé à l'avoine et un croissant frais, s'il vous plaît.", "アイスオーツラテと焼きたてのクロワッサンをください。"),
                    nativeHint = if (isBn) "কফি ও ক্রোয়াসাঁ অর্ডার করুন" else "Order a drink & pastry politely"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "What spices do you recommend for a quick home-cooked pasta?", "¿Qué especias recomiendas para una pasta casera rápida?", "Quelles épices recommandez-vous pour des pâtes maison rapides ?", "手早く作れるパスタにおすすめのスパイスは何ですか？"),
                    nativeHint = if (isBn) "রান্নার মশলা সম্পর্কে জিজ্ঞাসা করুন" else "Ask for a cooking recommendation"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "I love cooking spicy curry on weekends with fresh herbs.", "Me encanta cocinar curry picante los fines de semana con hierbas frescas.", "J'adore cuisiner du curry épicé le week-end avec des herbes fraîches.", "週末に新鮮なハーブでスパイシーなカレーを作るのが好きです。"),
                    nativeHint = if (isBn) "আপনার প্রিয় রান্নার কথা বলুন" else "Share a dish you love cooking"
                )
            )
            "airport_travel" -> listOf(
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "Hello, I would like to check in one bag and request a window seat.", "Hola, quiero facturar una maleta y pedir un asiento de ventanilla.", "Bonjour, je voudrais enregistrer un bagage et demander un siège hublot.", "こんにちは、荷物を一つ預けて窓側の席をお願いしたいです。"),
                    nativeHint = if (isBn) "ব্যাগ চেক-ইন ও জানালার সিট চান" else "Check luggage & ask for window seat"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "Which gate does the Tokyo flight board from, and when?", "¿En qué puerta embarca el vuelo a Tokio y a qué hora?", "À quelle porte embarque le vol pour Tokyo et à quelle heure ?", "東京行きの便は何番ゲートから何時に搭乗ですか？"),
                    nativeHint = if (isBn) "বোর্ডিং গেট ও সময় জানুন" else "Confirm gate & boarding time"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "Will my luggage transfer automatically to my connecting flight?", "¿Mi equipaje se transferirá automáticamente a mi vuelo de conexión?", "Mes bagages seront-ils transférés automatiquement vers ma correspondance ?", "荷物は乗り継ぎ便に自動で移動されますか？"),
                    nativeHint = if (isBn) "কানেক্টিং ফ্লাইটে ব্যাগ ট্রান্সফার জানুন" else "Ask about luggage transfer"
                )
            )
            "job_interview" -> listOf(
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "I recently led a mobile app launch that improved user retention by twenty percent.", "Recientemente lideré el lanzamiento de una app móvil que mejoró la retención.", "J'ai récemment dirigé le lancement d'une application mobile avec succès.", "最近モバイルアプリのリリースを主導し、継続率を大きく改善しました。"),
                    nativeHint = if (isBn) "আপনার সাম্প্রতিক প্রজেক্টের সাফল্য বলুন" else "Highlight a recent project achievement"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "What does success look like for this role during the first ninety days?", "¿Cómo se mide el éxito en este puesto durante los primeros noventa días?", "À quoi ressemble la réussite à ce poste durant les quatre-vingt-dix premiers jours ?", "最初の90日間でこの役割の成功はどのように評価されますか？"),
                    nativeHint = if (isBn) "কাজের প্রত্যাশা সম্পর্কে প্রশ্ন করুন" else "Ask a thoughtful question to the interviewer"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "I enjoy collaborating across teams to turn complex problems into simple tools.", "Disfruto colaborar entre equipos para resolver problemas complejos.", "J'aime collaborer entre équipes pour résoudre des problèmes complexes.", "チーム間で協力し、複雑な課題をシンプルに解決するのが得意です。"),
                    nativeHint = if (isBn) "আপনার দলগত কাজের শক্তি তুলে ধরুন" else "Describe your teamwork strengths"
                )
            )
            else -> listOf(
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "I am practicing daily so I can speak more naturally and confidently.", "Practico todos los días para hablar con más naturalidad y confianza.", "Je pratique chaque jour pour parler plus naturellement et avec confiance.", "より自然に自信を持って話せるよう毎日練習しています。"),
                    nativeHint = if (isBn) "প্রতিদিন অনুশীলনের কথা বলুন" else "Talk about your daily learning goal"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "Could you explain that expression again at a slightly slower pace?", "¿Podrías explicar esa expresión de nuevo un poco más despacio?", "Pourriez-vous expliquer cette expression un peu plus lentement ?", "その表現をもう少しゆっくり説明していただけますか？"),
                    nativeHint = if (isBn) "ধীরে ব্যাখ্যা করতে অনুরোধ করুন" else "Ask the tutor to slow down & clarify"
                ),
                InspireSuggestion(
                    targetReply = localizedLine(targetCode, "That makes sense! How would a native speaker say this in casual conversation?", "¡Tiene sentido! ¿Cómo lo diría un nativo en una charla casual?", "C'est clair ! Comment un natif dirait-il cela dans une discussion informelle ?", "なるほど！日常会話ではネイティブはどう言いますか？"),
                    nativeHint = if (isBn) "স্বাভাবিক কথ্য রূপ জানতে চান" else "Ask for a casual native alternative"
                )
            )
        }
    }

    private fun buildLectureGreeting(targetCode: String, topic: String, level: LanguageLevel): String =
        when (targetCode) {
            "es" -> "¡Hola! Bienvenido a nuestra lección de $topic. Hoy practicaremos expresiones claras y naturales para el nivel ${level.cefr}. ¿Estás listo para escuchar y repetir?"
            "fr" -> "Bonjour ! Bienvenue dans notre leçon sur $topic. Aujourd'hui, nous allons découvrir des expressions naturelles pour le niveau ${level.cefr}. Prêt à répéter après moi ?"
            "ja" -> "こんにちは！今日は${topic}のレッスンです。レベル${level.cefr}に合わせて自然な表現を一緒に練習しましょう。準備はいいですか？"
            "de" -> "Hallo! Willkommen zu unserer Lektion über $topic. Heute üben wir natürliche Ausdrücke für Niveau ${level.cefr}. Bist du bereit?"
            "bn" -> "নমস্কার! আজকের $topic পাঠে স্বাগতম। চলুন ${level.cefr} স্তরের জন্য স্বাভাবিক ও সুন্দর বাক্য অনুশীলন করি। আপনি কি প্রস্তুত?"
            else -> "Welcome back! Today's ${level.label} mini-lecture focuses on $topic. Listen carefully to the stress and cadence of each phrase—ready to practice together?"
        }

    private fun buildPracticeGreeting(targetCode: String, topic: String, level: LanguageLevel): String =
        when (targetCode) {
            "es" -> "¡Hora de practicar! Hablemos sobre $topic. Cuéntame en una o dos frases qué te gusta más de este tema."
            "fr" -> "Passons à la pratique ! Parlons de $topic. Dis-moi en une ou deux phrases ce que tu préfères à ce sujet ?"
            "ja" -> "実践タイムです！${topic}について話しましょう。あなたの好きなことや最近の体験を教えてください。"
            "de" -> "Zeit zum Üben! Lass uns über $topic sprechen. Was gefällt dir an diesem Thema am meisten?"
            "bn" -> "চলুন অনুশীলন শুরু করি! $topic সম্পর্কে আপনার অভিজ্ঞতা বা পছন্দের কথা এক-দুই বাক্যে বলুন তো?"
            else -> "Let's jump right into speaking practice about $topic! Tell me in one or two sentences—what's something you enjoyed recently related to $topic?"
        }

    private fun buildNativeExplanation(
        nativeCode: String,
        nativeName: String,
        targetName: String,
        topic: String,
        mode: TutorMode,
        level: LanguageLevel
    ): String {
        val modeLabelBn = if (mode == TutorMode.LECTURE) "লেকচার মোড" else "প্র্যাকটিস মোড"
        return when (nativeCode) {
            "bn" -> "[$modeLabelBn · নমুনা পাঠ] আজ আমরা $targetName ভাষায় '$topic' নিয়ে ${level.label} (${level.cefr}) স্তরের গুরুত্বপূর্ণ বাক্য শিখব। নিচের প্রতিটি বাক্য শুনুন এবং বলার চেষ্টা করুন:"
            "es" -> "[Muestra · ${level.cefr}] Hoy practicaremos '$topic' en $targetName. Escucha la pronunciación y usa las frases clave de abajo:"
            "hi" -> "[सैंपल पाठ · ${level.cefr}] आज हम $targetName में '$topic' के लिए उपयोगी वाक्य सीखेंगे। नीचे दिए गए वाक्यों को सुनें और दोहराएं:"
            "fr" -> "[Exemple · ${level.cefr}] Aujourd'hui nous travaillons '$topic' en $targetName. Écoutez chaque expression et répétez à voix haute :"
            "ar" -> "[درس تجريبي · ${level.cefr}] سنتدرب اليوم على موضوع '$topic' باللغة $targetName. استمع إلى العبارات التالية وكررها:"
            else -> "[Sample Lesson in $nativeName · ${level.cefr}] Today we are practicing '$topic' in $targetName. Study these core phrases and tap Play Audio to hear Aria speak them:"
        }
    }

    private fun buildGentleFeedback(
        userText: String,
        wordCount: Int,
        nativeCode: String,
        targetName: String,
        studyInNative: Boolean,
        level: LanguageLevel
    ): String {
        val endsWithPunctuation = userText.endsWith(".") || userText.endsWith("?") || userText.endsWith("!")
        return if (studyInNative && nativeCode == "bn") {
            when {
                wordCount <= 2 -> "টিপস: সুন্দর শুরু! বাক্যটি আরেকটু বড় করে একটি কারণ বা সময় যোগ করলে $targetName আরও স্বাভাবিক শোনাবে।"
                !endsWithPunctuation -> "টিপস: চমৎকার প্রকাশভঙ্গি (${level.cefr})! কথা বলার শেষে স্বরের ওঠানামা (intonation) একটু স্পষ্ট রাখলে উচ্চারণ আরও নিখুঁত হবে।"
                else -> "চমৎকার গঠন! আপনার বাক্যটি পরিষ্কার এবং ${level.label} স্তরের জন্য দারুণ উপযোগী।"
            }
        } else {
            when {
                wordCount <= 2 -> "Tip: Great start! Try expanding your sentence with a reason or detail to sound more natural in $targetName."
                !endsWithPunctuation -> "Tip: Nice phrasing for ${level.cefr}! Pay attention to clear rising/falling intonation at the end of your phrase."
                else -> "Great sentence structure! Clear, natural phrasing for the ${level.label} level."
            }
        }
    }

    private fun rolePlayOpeningLine(targetCode: String, scenarioId: String): String = when (scenarioId) {
        "cafe_cooking" -> localizedLine(
            targetCode,
            "Good morning and welcome to Hearth & Bean! What can I brew for you today, or would you like to hear our chef's daily special?",
            "¡Buenos días! Bienvenido a nuestra cafetería. ¿Qué te preparo hoy o quieres conocer el plato especial del chef?",
            "Bonjour et bienvenue au café ! Que puis-je vous préparer aujourd'hui ? Voulez-vous découvrir le plat du jour ?",
            "おはようございます！カフェへようこそ。今日は何をお作りしましょうか？本日のスペシャルもございますよ。"
        )
        "airport_travel" -> localizedLine(
            targetCode,
            "Hello! Welcome to the international check-in counter. May I see your passport, and are you checking any luggage today?",
            "¡Hola! Bienvenido al mostrador internacional. ¿Puedo ver su pasaporte? ¿Va a facturar equipaje hoy?",
            "Bonjour ! Bienvenue au comptoir d'enregistrement. Puis-je voir votre passeport ? Avez-vous des bagages à enregistrer ?",
            "こんにちは！国際線チェックインカウンターへようこそ。パスポートを拝見できますか？お預けの荷物はありますか？"
        )
        "job_interview" -> localizedLine(
            targetCode,
            "Thanks so much for joining me today! I loved reading your background—could you start by telling me about a project you are really proud of?",
            "¡Muchas gracias por venir hoy! Me encantó tu perfil. ¿Podrías contarme sobre un proyecto del que estés muy orgulloso?",
            "Merci beaucoup d'être venu aujourd'hui ! Pouvez-vous commencer par me parler d'un projet dont vous êtes particulièrement fier ?",
            "本日はお越しいただきありがとうございます！まず、あなたが特に誇りに思っているプロジェクトについて教えていただけますか？"
        )
        else -> localizedLine(
            targetCode,
            "Welcome in! Feel free to look around our weekend boutique. Are you shopping for something specific today, or just browsing?",
            "¡Bienvenido! Siéntete libre de mirar nuestra tienda. ¿Buscas algo especial hoy o solo estás echando un vistazo?",
            "Bienvenue ! N'hésitez pas à regarder autour de vous. Cherchez-vous quelque chose en particulier aujourd'hui ?",
            "いらっしゃいませ！ごゆっくりご覧ください。今日は何かお探しのものがありますか？"
        )
    }

    private fun rolePlayFollowUp(targetCode: String, scenarioId: String, turnCount: Int): String = when (scenarioId) {
        "cafe_cooking" -> if (turnCount % 2 == 0) {
            localizedLine(
                targetCode,
                "Wonderful choice! Would you like it hot or iced, and do you enjoy cooking similar flavors at home?",
                "¡Excelente elección! ¿Lo prefieres caliente o frío? ¿Te gusta cocinar sabores parecidos en casa?",
                "Excellent choix ! Vous le préférez chaud ou glacé ? Aimez-vous cuisiner ce genre de saveurs chez vous ?",
                "素晴らしい選択ですね！ホットとアイスどちらにしますか？ご自宅でもお料理されますか？"
            )
        } else {
            localizedLine(
                targetCode,
                "That sounds delicious! I'll prepare your order right away—anything else I can add for you?",
                "¡Suena delicioso! Preparo tu pedido enseguida. ¿Deseas agregar algo más?",
                "Cela a l'air délicieux ! Je prépare votre commande tout de suite. Autre chose ?",
                "美味しそうですね！すぐにお作りします。他に追加はございますか？"
            )
        }
        "airport_travel" -> localizedLine(
            targetCode,
            "Got it! I've reserved seat 14A by the window for you. Your flight boards at Gate A12 in forty minutes—do you need directions to security?",
            "¡Perfecto! Reservé el asiento 14A junto a la ventana. Su vuelo embarca en la puerta A12 en cuarenta minutos. ¿Necesita indicaciones?",
            "Parfait ! Je vous ai attribué le siège 14A côté hublot. Embarquement porte A12 dans quarante minutes. Avez-vous besoin d'aide ?",
            "かしこまりました！窓側の14A席をご用意しました。搭乗はA12ゲートで40分後です。保安検査場へのご案内は必要ですか？"
        )
        "job_interview" -> localizedLine(
            targetCode,
            "That's a really compelling impact! How did you handle unexpected challenges during that project, and what would you like to ask me about our team?",
            "¡Qué gran impacto! ¿Cómo manejaste los desafíos inesperados en ese proyecto y qué te gustaría preguntarme sobre el equipo?",
            "C'est un impact impressionnant ! Comment avez-vous géré les imprévus, et quelles questions avez-vous sur notre équipe ?",
            "素晴らしい成果ですね！途中で起きた課題はどう乗り越えましたか？また、私たちのチームについて質問はありますか？"
        )
        else -> localizedLine(
            targetCode,
            "Great eye! We have that in medium and large, and if you pair two items today I can offer a fifteen percent discount. Would you like to try it on?",
            "¡Buen gusto! Lo tenemos en talla mediana y grande, y si llevas dos artículos hay un quince por ciento de descuento. ¿Quieres probártelo?",
            "Très bon choix ! Nous l'avons en moyen et grand, avec quinze pour cent de réduction pour deux articles. Voulez-vous l'essayer ?",
            "お目が高いですね！MとLサイズがあり、2点以上で15%割引になります。ご試着されますか？"
        )
    }

    private fun lessonFollowUp(targetCode: String, mode: TutorMode, level: LanguageLevel, turnCount: Int): String =
        if (mode == TutorMode.LECTURE) {
            localizedLine(
                targetCode,
                "Awesome work! Now let's build on that idea with a more expressive ${level.label} phrasing. Notice how smoothly the words link together—can you try saying the next phrase?",
                "¡Excelente trabajo! Ahora ampliemos esa idea con una frase más expresiva. ¿Puedes intentar decir la siguiente expresión?",
                "Bravo ! Développons maintenant cette idée avec une tournure encore plus naturelle. Pouvez-vous essayer la phrase suivante ?",
                "素晴らしいです！次はもう少し自然な表現に広げてみましょう。次のフレーズを言ってみてください。"
            )
        } else {
            if (turnCount % 2 == 0) {
                localizedLine(
                    targetCode,
                    "I love hearing that! What made you interested in it, and how often do you get to enjoy it during a busy week?",
                    "¡Me encanta escuchar eso! ¿Qué despertó tu interés y con qué frecuencia lo disfrutas?",
                    "J'adore entendre ça ! Qu'est-ce qui t'a donné envie d'essayer, et à quelle fréquence en profites-tu ?",
                    "素敵ですね！どうしてそれに興味を持ったのですか？普段どれくらいの頻度で楽しんでいますか？"
                )
            } else {
                localizedLine(
                    targetCode,
                    "You expressed that so clearly! If you had to recommend one tip to a friend trying it for the first time, what would you say?",
                    "¡Lo expresaste con mucha claridad! Si tuvieras que dar un consejo a un amigo, ¿qué le dirías?",
                    "Tu l'as expliqué très clairement ! Quel conseil donnerais-tu à un ami qui débute ?",
                    "とても分かりやすく話せましたね！初めて挑戦する友達に一つアドバイスするなら何と言いますか？"
                )
            }
        }

    private fun rolePlayStarterPhrases(targetCode: String, nativeCode: String, scenarioId: String): List<VocabPhrase> {
        val isBn = nativeCode == "bn"
        return when (scenarioId) {
            "cafe_cooking" -> listOf(
                VocabPhrase(
                    targetText = localizedLine(targetCode, "Could I have a medium oat latte, please?", "¿Me pone un café con leche de avena, por favor?", "Je voudrais un latte à l'avoine, s'il vous plaît.", "オーツラテのMサイズをいただけますか？"),
                    pronunciation = if (targetCode == "en") "/kʊd aɪ hæv ə ˈmiːdiəm oʊt ˈlɑːteɪ pliːz/" else "Polite café request",
                    nativeMeaning = if (isBn) "আমি কি একটি মিডিয়াম ওট লাতে পেতে পারি?" else "Polite way to order a drink"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "What ingredients go into the daily special?", "¿Qué ingredientes lleva el especial del día?", "Quels ingrédients composent le plat du jour ?", "本日のスペシャルにはどんな食材が入っていますか？"),
                    pronunciation = if (targetCode == "en") "/wʌt ɪnˈɡriːdiənts ɡoʊ ˈɪntuː ðə ˈdeɪli ˈspeʃəl/" else "Asking about ingredients",
                    nativeMeaning = if (isBn) "আজকের স্পেশাল খাবারে কী কী উপাদান আছে?" else "Ask about ingredients in a dish"
                )
            )
            "airport_travel" -> listOf(
                VocabPhrase(
                    targetText = localizedLine(targetCode, "I'd like a window seat if available.", "Quisiera un asiento de ventanilla si está disponible.", "Je souhaiterais un siège côté hublot si possible.", "空いていれば窓側の席をお願いします。"),
                    pronunciation = if (targetCode == "en") "/aɪd laɪk ə ˈwɪndoʊ siːt ɪf əˈveɪləbəl/" else "Seat preference",
                    nativeMeaning = if (isBn) "সম্ভব হলে আমি জানালার পাশের সিট নিতে চাই।" else "Requesting a window seat"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "Where is the boarding gate for my flight?", "¿Dónde está la puerta de embarque de mi vuelo?", "Où se trouve la porte d'embarquement ?", "私の便の搭乗ゲートはどこですか？"),
                    pronunciation = if (targetCode == "en") "/weər ɪz ðə ˈbɔːrdɪŋ ɡeɪt/" else "Gate inquiry",
                    nativeMeaning = if (isBn) "আমার ফ্লাইটের বোর্ডিং গেট কোথায়?" else "Asking for the boarding gate"
                )
            )
            "job_interview" -> listOf(
                VocabPhrase(
                    targetText = localizedLine(targetCode, "My key strength is turning ideas into reliable products.", "Mi mayor fortaleza es convertir ideas en productos.", "Ma force principale est de concrétiser des idées.", "私の強みはアイデアを確かな製品にすることです。"),
                    pronunciation = if (targetCode == "en") "/maɪ kiː streŋθ ɪz ˈtɜːrnɪŋ aɪˈdɪəz.../" else "Presenting strengths",
                    nativeMeaning = if (isBn) "আমার মূল শক্তি হলো ধারণাকে নির্ভরযোগ্য পণ্যে রূপান্তর করা।" else "Highlighting your core strength"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "How does the team collaborate on new goals?", "¿Cómo colabora el equipo en nuevos objetivos?", "Comment l'équipe collabore-t-elle sur les objectifs ?", "チームは新しい目標にどう協力しますか？"),
                    pronunciation = if (targetCode == "en") "/haʊ dʌz ðə tiːm kəˈlæbəreɪt/" else "Teamwork question",
                    nativeMeaning = if (isBn) "নতুন লক্ষ্যে টিম কীভাবে একসাথে কাজ করে?" else "Smart interview question"
                )
            )
            else -> listOf(
                VocabPhrase(
                    targetText = localizedLine(targetCode, "Do you have this in a slightly smaller size?", "¿Tiene esto en una talla un poco más pequeña?", "Avez-vous ceci dans une taille plus petite ?", "これのもう少し小さいサイズはありますか？"),
                    pronunciation = if (targetCode == "en") "/duː juː hæv ðɪs ɪn ə ˈsmɔːlər saɪz/" else "Asking for size",
                    nativeMeaning = if (isBn) "এটির একটু ছোট সাইজ কি আপনাদের কাছে আছে?" else "Ask for another size"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "Is there any bundle discount if I take two?", "¿Hay algún descuento si llevo dos?", "Y a-t-il une remise si j'en prends deux ?", "2つ買うとセット割引はありますか？"),
                    pronunciation = if (targetCode == "en") "/ɪz ðeər ˈeni ˈbʌndəl ˈdɪskaʊnt/" else "Polite negotiation",
                    nativeMeaning = if (isBn) "দুইটি নিলে কি কোনো বিশেষ ছাড় আছে?" else "Ask politely about a discount"
                )
            )
        }
    }

    private fun samplePhrasesFor(
        targetCode: String,
        nativeCode: String,
        topic: String,
        level: LanguageLevel
    ): List<VocabPhrase> {
        val isBn = nativeCode == "bn"
        return when (level) {
            LanguageLevel.BEGINNER, LanguageLevel.ELEMENTARY -> listOf(
                VocabPhrase(
                    targetText = localizedLine(targetCode, "Could you repeat that slowly, please?", "¿Puedes repetir eso despacio, por favor?", "Pouvez-vous répéter lentement, s'il vous plaît ?", "もう少しゆっくり繰り返していただけますか？"),
                    pronunciation = if (targetCode == "en") "/kʊd juː rɪˈpiːt ðæt ˈsloʊli pliːz/" else "Helpful beginner phrase",
                    nativeMeaning = if (isBn) "দয়া করে কথাটি আরেকবার ধীরে বলবেন?" else "Ask someone to repeat slowly"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "I really enjoy learning about $topic.", "Realmente disfruto aprender sobre $topic.", "J'aime beaucoup apprendre sur $topic.", "${topic}について学ぶのがとても楽しいです。"),
                    pronunciation = if (targetCode == "en") "/aɪ ˈrɪəli ɪnˈdʒɔɪ ˈlɜːrnɪŋ əˈbaʊt.../" else "Expressing interest",
                    nativeMeaning = if (isBn) "আমি '$topic' সম্পর্কে শিখতে খুব পছন্দ করি।" else "Express enthusiasm for a topic"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "What do you recommend for beginners?", "¿Qué recomiendas para principiantes?", "Que recommandez-vous aux débutants ?", "初心者におすすめは何ですか？"),
                    pronunciation = if (targetCode == "en") "/wʌt duː juː ˌrekəˈmend/" else "Asking recommendation",
                    nativeMeaning = if (isBn) "নতুনদের জন্য আপনি কী পরামর্শ দেবেন?" else "Ask for a beginner tip"
                )
            )
            else -> listOf(
                VocabPhrase(
                    targetText = localizedLine(targetCode, "What fascinates me most about $topic is the creativity behind it.", "Lo que más me fascina de $topic es su creatividad.", "Ce qui me fascine le plus dans $topic, c'est la créativité.", "${topic}で一番魅力的なのはその創造性です。"),
                    pronunciation = if (targetCode == "en") "/wʌt ˈfæsɪneɪts miː moʊst.../" else "Natural opinion opener",
                    nativeMeaning = if (isBn) "'$topic'-এর পেছনের সৃজনশীলতা আমাকে সবচেয়ে বেশি আকর্ষণ করে।" else "Share a nuanced personal perspective"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "From my experience, small daily habits make the biggest difference.", "En mi experiencia, los pequeños hábitos diarios marcan la diferencia.", "D'après mon expérience, les petites habitudes font toute la différence.", "私の経験では、毎日の小さな習慣が一番大きな違いを生みます。"),
                    pronunciation = if (targetCode == "en") "/frʌm maɪ ɪkˈspɪriəns.../" else "Conversational connector",
                    nativeMeaning = if (isBn) "আমার অভিজ্ঞতায়, প্রতিদিনের ছোট অভ্যাসই সবচেয়ে বড় পরিবর্তন আনে।" else "Express an insight smoothly"
                ),
                VocabPhrase(
                    targetText = localizedLine(targetCode, "How does that compare to what you expected?", "¿Cómo se compara eso con lo que esperabas?", "Comment cela se compare-t-il à vos attentes ?", "それはあなたの予想と比べてどうでしたか？"),
                    pronunciation = if (targetCode == "en") "/haʊ dʌz ðæt kəmˈpeər/" else "Engaging follow-up",
                    nativeMeaning = if (isBn) "আপনার প্রত্যাশার তুলনায় এটি কেমন ছিল?" else "Keep a deep conversation going"
                )
            )
        }
    }

    private fun localizedLine(
        targetCode: String,
        en: String,
        es: String,
        fr: String,
        ja: String
    ): String = when (targetCode) {
        "es" -> es
        "fr" -> fr
        "ja" -> ja
        else -> en
    }
}
