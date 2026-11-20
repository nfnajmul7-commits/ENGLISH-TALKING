package com.example.data

import com.example.model.LanguageLevel
import com.example.model.LearningModule
import com.example.model.VocabPhrase

data class QuizQuestion(
    val id: String,
    val level: LanguageLevel,
    val questionBn: String,
    val questionEn: String,
    val options: List<String>, // e.g., ["A) ...", "B) ...", "C) ..."]
    val correctIndex: Int,
    val explanationBn: String,
    val explanationEn: String
)

data class GrammarTopicLesson(
    val key: String,
    val titleBn: String,
    val titleEn: String,
    val summaryBn: String,
    val summaryEn: String,
    val phrases: List<VocabPhrase>,
    val followUpRepliesBn: List<String>,
    val followUpRepliesEn: List<String>
)

/**
 * Curated multi-level (A1–C1) curriculum data across 8 learning modules, interactive quizzes,
 * grammar guides, and multilingual starter expressions.
 */
object TutorCurriculumBank {

    val grammarLessons: List<GrammarTopicLesson> = listOf(
        GrammarTopicLesson(
            key = "present",
            titleBn = "Present Tense (বর্তমান কাল)",
            titleEn = "Present Tense Rules",
            summaryBn = "প্রতিদিনের অভ্যাস বা সত্য কথা বলতে Present Simple (I go / He goes) এবং এখন চলছে এমন কাজে Present Continuous (I am going) ব্যবহার হয়।",
            summaryEn = "Use Present Simple for daily habits (I go / He goes) and Present Continuous for actions happening right now (I am going).",
            phrases = listOf(
                VocabPhrase("I practice English every morning.", "আই প্র্যাকটিস ইংলিশ এভরি মর্নিং।", "আমি প্রতিদিন সকালে ইংরেজি অনুশীলন করি। (অভ্যাস)"),
                VocabPhrase("She works in a hospital.", "শি ওয়ার্কস ইন আ হসপিটাল।", "তিনি একটি হাসপাতালে কাজ করেন। (He/She হলে verb-এ s/es)"),
                VocabPhrase("I am learning new words right now.", "আই অ্যাম লার্নিং নিউ ওয়ার্ডস রাইট নাউ।", "আমি এখন নতুন শব্দ শিখছি। (চলমান কাজ)"),
                VocabPhrase("Do you speak English?", "ডু ইউ স্পিক ইংলিশ?", "আপনি কি ইংরেজি বলেন? (প্রশ্ন করতে Do/Does)")
            ),
            followUpRepliesBn = listOf("Past Tense শেখান", "Future Tense শেখান", "Preposition (in/on/at)", "কুইজ দিন"),
            followUpRepliesEn = listOf("Teach Past Tense", "Teach Future Tense", "Prepositions (in/on/at)", "Quiz me")
        ),
        GrammarTopicLesson(
            key = "past",
            titleBn = "Past Tense (অতীত কাল)",
            titleEn = "Past Tense Rules",
            summaryBn = "অতীতে শেষ হয়ে যাওয়া কাজ বলতে Verb-এর Past রূপ (went, ate, worked) বসে। তবে 'did' ব্যবহার করলে মূল Verb (go, eat, work) বসে!",
            summaryEn = "Use the past form (went, ate, worked) for finished actions. After 'did' or 'didn't', always return to the base verb!",
            phrases = listOf(
                VocabPhrase("I went to the market yesterday.", "আই ওয়েন্ট টু দ্য মার্কেট ইয়েস্টারডে।", "আমি গতকাল বাজারে গিয়েছিলাম। (go → went)"),
                VocabPhrase("Did you sleep well last night?", "ডিড ইউ স্লিপ ওয়েল লাস্ট নাইট?", "গত রাতে আপনার ঘুম ভালো হয়েছে? (did + base verb)"),
                VocabPhrase("I didn't see your message.", "আই ডিডন্ট সি ইয়োর মেসেজ।", "আমি আপনার মেসেজটি দেখিনি। (didn't + see)"),
                VocabPhrase("We were talking about travel.", "উই অয়্যার টকিং অ্যাবাউট ট্রাভেল।", "আমরা ভ্রমণ নিয়ে কথা বলছিলাম। (Past Continuous)")
            ),
            followUpRepliesBn = listOf("Future Tense শেখান", "Present Tense শেখান", "Polite Modals (Could/Would)", "কুইজ দিন"),
            followUpRepliesEn = listOf("Teach Future Tense", "Teach Present Tense", "Polite Modals", "Quiz me")
        ),
        GrammarTopicLesson(
            key = "future",
            titleBn = "Future Tense (ভবিষ্যৎ কাল)",
            titleEn = "Future Tense Rules",
            summaryBn = "তাৎক্ষণিক সিদ্ধান্ত বা প্রতিশ্রুতিতে 'will' এবং আগে থেকে ঠিক করা পরিকল্পনায় 'going to' ব্যবহার করলে ইংরেজি একদম স্বাভাবিক শোনায়।",
            summaryEn = "Use 'will' for quick decisions or promises, and 'going to' for plans you already decided.",
            phrases = listOf(
                VocabPhrase("I will call you in ten minutes.", "আই উইল কল ইউ ইন টেন মিনিটস।", "আমি দশ মিনিটের মধ্যে আপনাকে ফোন করব। (প্রতিশ্রুতি)"),
                VocabPhrase("I'm going to visit Dhaka next week.", "আইম গোয়িং টু ভিজিট ঢাকা নেক্সট উইক।", "আমি আগামী সপ্তাহে ঢাকা যাব। (আগের পরিকল্পনা)"),
                VocabPhrase("Will you join us for dinner?", "উইল ইউ জয়েন আস ফর ডিনার?", "আপনি কি আমাদের সাথে রাতের খাবারে যোগ দেবেন?"),
                VocabPhrase("Everything will be fine.", "এভরিথিং উইল বি ফাইন।", "সবকিছু ঠিক হয়ে যাবে।")
            ),
            followUpRepliesBn = listOf("Preposition (in/on/at)", "Polite Modals (Could/Would)", "Past Tense শেখান", "কুইজ দিন"),
            followUpRepliesEn = listOf("Prepositions (in/on/at)", "Polite Modals", "Teach Past Tense", "Quiz me")
        ),
        GrammarTopicLesson(
            key = "preposition",
            titleBn = "Prepositions: IN, ON, AT এর সহজ নিয়ম",
            titleEn = "Prepositions: IN, ON, AT",
            summaryBn = "মনে রাখার সহজ সূত্র: বড় সময়/জায়গায় IN (বছর, মাস, দেশ), নির্দিষ্ট দিন/তলে ON (বার, তারিখ, টেবিল), আর একদম নির্দিষ্ট সময়/স্থানে AT (৭টায়, দরজায়)।",
            summaryEn = "Easy rule: IN for months/years/cities, ON for days/dates/surfaces, and AT for exact times and specific points.",
            phrases = listOf(
                VocabPhrase("I wake up at 7:00 AM.", "আই ওয়েক আপ অ্যাট সেভেন এএম।", "আমি সকাল ৭টায় ঘুম থেকে উঠি। (নির্দিষ্ট সময় = at)"),
                VocabPhrase("The meeting is on Monday.", "দ্য মিটিং ইজ অন মানডে।", "মিটিংটি সোমবার। (দিন বা তারিখ = on)"),
                VocabPhrase("I live in Bangladesh.", "আই লিভ ইন বাংলাদেশ।", "আমি বাংলাদেশে থাকি। (দেশ/শহর/মাস = in)"),
                VocabPhrase("Put the phone on the table.", "পুট দ্য ফোন অন দ্য টেবিল।", "ফোনটি টেবিলের ওপর রাখো। (কোনো কিছুর তলে = on)")
            ),
            followUpRepliesBn = listOf("Polite Modals (Could/Would)", "Present Tense শেখান", "Past Tense শেখান", "কুইজ দিন"),
            followUpRepliesEn = listOf("Polite Modals", "Teach Present Tense", "Teach Past Tense", "Quiz me")
        ),
        GrammarTopicLesson(
            key = "modals",
            titleBn = "ভদ্র ইংরেজি: Could, Would, May",
            titleEn = "Polite Modals: Could, Would, May",
            summaryBn = "ইংরেজিতে সরাসরি 'Give me' না বলে 'Could you...' বা 'I would like...' বললে কথা অনেক ভদ্র ও মার্জিত শোনায়।",
            summaryEn = "Instead of direct commands like 'Give me', use 'Could you...', 'Would you mind...', or 'May I...' for polite natural English.",
            phrases = listOf(
                VocabPhrase("Could you please help me with this?", "কুড ইউ প্লিজ হেল্প মি উইথ দিস?", "দয়া করে আমাকে এতে একটু সাহায্য করবেন?"),
                VocabPhrase("I would like to order a coffee.", "আই উড লাইক টু অর্ডার আ কফি।", "আমি একটি কফি অর্ডার করতে চাই।"),
                VocabPhrase("May I ask you a quick question?", "মে আই আস্ক ইউ আ কুইক কোয়েশ্চন?", "আমি কি আপনাকে একটি ছোট প্রশ্ন করতে পারি?"),
                VocabPhrase("Would you mind repeating that?", "উড ইউ মাইন্ড রিপিটিং দ্যাট?", "কিছু মনে না করলে কথাটা আরেকবার বলবেন?")
            ),
            followUpRepliesBn = listOf("Present Tense শেখান", "Preposition (in/on/at)", "Past Tense শেখান", "কুইজ দিন"),
            followUpRepliesEn = listOf("Teach Present Tense", "Prepositions (in/on/at)", "Teach Past Tense", "Quiz me")
        )
    )

    val quizBank: List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "q_a1_morning",
            level = LanguageLevel.BEGINNER,
            questionBn = "সকালে কারো সাথে দেখা হলে ভদ্রভাবে কী বলবেন?",
            questionEn = "What is the polite way to greet someone in the morning?",
            options = listOf("A) Good morning!", "B) Good night!", "C) See you later!"),
            correctIndex = 0,
            explanationBn = "সকালে দেখা হলে 'Good morning!' বলা হয়। 'Good night' শুধু রাতে ঘুমাতে যাওয়ার আগে বিদায় নেওয়ার সময় বলা হয়।",
            explanationEn = "'Good morning!' is used to greet people in the morning, while 'Good night!' is only used when saying goodbye at night."
        ),
        QuizQuestion(
            id = "q_a1_did_go",
            level = LanguageLevel.BEGINNER,
            questionBn = "কোন বাক্যটি গ্রামার অনুযায়ী একদম সঠিক?",
            questionEn = "Which sentence is grammatically correct?",
            options = listOf("A) Did you went home?", "B) Did you go home?", "C) Did you going home?"),
            correctIndex = 1,
            explanationBn = "'Did' নিজেই অতীত কাল বোঝায়, তাই এর পর verb-এর মূল রূপ 'go' বসে — 'Did you go home?'।",
            explanationEn = "After 'did', always use the base form of the verb: 'Did you go home?'."
        ),
        QuizQuestion(
            id = "q_a1_preposition_time",
            level = LanguageLevel.BEGINNER,
            questionBn = "শূন্যস্থান পূরণ করুন: \"The class starts ___ 8:00 AM.\"",
            questionEn = "Fill in the blank: \"The class starts ___ 8:00 AM.\"",
            options = listOf("A) in", "B) on", "C) at"),
            correctIndex = 2,
            explanationBn = "ঘড়ির নির্দিষ্ট সময়ের আগে সবসময় 'at' বসে (at 8:00 AM)।",
            explanationEn = "Always use 'at' before an exact clock time (at 8:00 AM)."
        ),
        QuizQuestion(
            id = "q_a2_not_sure",
            level = LanguageLevel.ELEMENTARY,
            questionBn = "\"Not sure if you heard the news\" — এর স্বাভাবিক বাংলা অর্থ কী?",
            questionEn = "What does \"Not sure if you heard the news\" mean?",
            options = listOf("A) জানি না আপনি খবরটা শুনেছেন কিনা", "B) আপনি খবরটা শোনেননি", "C) আমি নিশ্চিত আপনি শুনেছেন"),
            correctIndex = 0,
            explanationBn = "'Not sure if you...' মানে 'নিশ্চিত নই আপনি ... কিনা' বা 'জানি না আপনি ... কিনা'। ইংরেজি কথোপকথনে এটি খুব ব্যবহৃত হয়!",
            explanationEn = "'Not sure if you...' is a casual way to say 'I am not sure whether you...'."
        ),
        QuizQuestion(
            id = "q_a2_agree",
            level = LanguageLevel.ELEMENTARY,
            questionBn = "'আমি তোমার সাথে একমত' — এর সঠিক ইংরেজি কোনটি?",
            questionEn = "Which is the correct way to express agreement?",
            options = listOf("A) I am agree with you.", "B) I agree with you.", "C) I agreeing you."),
            correctIndex = 1,
            explanationBn = "'agree' নিজেই একটি verb, তাই এর আগে 'am' বসে না। সঠিক বাক্য হলো 'I agree with you.'।",
            explanationEn = "'Agree' is already a verb, so we say 'I agree with you' (never 'I am agree')."
        ),
        QuizQuestion(
            id = "q_a2_on_monday",
            level = LanguageLevel.ELEMENTARY,
            questionBn = "শূন্যস্থান পূরণ করুন: \"We have an interview ___ Monday.\"",
            questionEn = "Fill in the blank: \"We have an interview ___ Monday.\"",
            options = listOf("A) on", "B) at", "C) in"),
            correctIndex = 0,
            explanationBn = "সপ্তাহের যেকোনো বার (Monday, Friday) ও তারিখের আগে 'on' বসে।",
            explanationEn = "Use 'on' before days of the week and specific dates (on Monday)."
        ),
        QuizQuestion(
            id = "q_b1_look_forward",
            level = LanguageLevel.INTERMEDIATE,
            questionBn = "সঠিক বাক্যটি বেছে নিন:",
            questionEn = "Choose the natural, correct sentence:",
            options = listOf("A) I look forward to meet you.", "B) I look forward to meeting you.", "C) I look forward meeting you."),
            correctIndex = 1,
            explanationBn = "'look forward to' এর পর 'to' একটি preposition, তাই এরপর verb+ing ('meeting') বসে!",
            explanationEn = "In 'look forward to', 'to' is a preposition, so it is followed by verb + -ing ('meeting')."
        ),
        QuizQuestion(
            id = "q_b1_since_for",
            level = LanguageLevel.INTERMEDIATE,
            questionBn = "\"I have lived here ___ five years.\" — শূন্যস্থানে কী বসবে?",
            questionEn = "Complete: \"I have lived here ___ five years.\"",
            options = listOf("A) since", "B) for", "C) from"),
            correctIndex = 1,
            explanationBn = "মোট সময়কাল (five years, two hours) বোঝাতে 'for' এবং শুরুর নির্দিষ্ট সময় (since 2020) বোঝাতে 'since' বসে।",
            explanationEn = "Use 'for' with a duration (five years) and 'since' with a starting point (since 2020)."
        ),
        QuizQuestion(
            id = "q_b2_idiom",
            level = LanguageLevel.UPPER_INTERMEDIATE,
            questionBn = "\"Let's circle back to this tomorrow\" — অফিসে এর মানে কী?",
            questionEn = "In a meeting, what does \"Let's circle back to this tomorrow\" mean?",
            options = listOf("A) কাজটা বাতিল করি", "B) আগামীকাল এ বিষয়ে আবার আলোচনা করি", "C) এখনই শেষ করি"),
            correctIndex = 1,
            explanationBn = "'Circle back' মানে কোনো বিষয়ে পরে আবার ফিরে এসে আলোচনা করা — কর্পোরেট ইংরেজিতে খুব জনপ্রিয়।",
            explanationEn = "'Circle back' means to return to a topic later to discuss or follow up on it."
        ),
        QuizQuestion(
            id = "q_c1_nuance",
            level = LanguageLevel.ADVANCED,
            questionBn = "কোনটি সবচেয়ে ভদ্র ও পেশাদারভাবে দ্বিমত প্রকাশ করে?",
            questionEn = "Which phrase disagrees most diplomatically in a leadership meeting?",
            options = listOf("A) You are completely wrong.", "B) I see your point, though we might consider another angle.", "C) That makes no sense."),
            correctIndex = 1,
            explanationBn = "'I see your point, though...' প্রথমে অন্যের মতকে সম্মান জানিয়ে তারপর নিজের ভিন্ন দৃষ্টিভঙ্গি তুলে ধরে।",
            explanationEn = "'I see your point, though...' acknowledges the other person before introducing an alternative view."
        )
    )

    fun modulePhrases(
        module: LearningModule,
        targetCode: String,
        nativeCode: String,
        level: LanguageLevel,
        topic: String
    ): List<VocabPhrase> {
        val isBn = nativeCode == "bn"
        return when (module) {
            LearningModule.CONVERSATION -> progressiveLessonPhrases(targetCode, nativeCode, level, topic)

            LearningModule.TRANSLATE -> listOf(
                VocabPhrase(
                    targetText = "Not sure if you heard about this.",
                    pronunciation = "নট শিওর ইফ ইউ হার্ড অ্যাবাউট দিস।",
                    nativeMeaning = if (isBn) "নিশ্চিত নই আপনি এ বিষয়ে শুনেছেন কিনা।" else "Casual way to introduce news or check if someone knows."
                ),
                VocabPhrase(
                    targetText = "What does this phrase mean?",
                    pronunciation = "হোয়াট ডাজ দিস ফ্রেজ মিন?",
                    nativeMeaning = if (isBn) "এই বাক্যাংশটির অর্থ কী?" else "Ask the meaning of any expression."
                ),
                VocabPhrase(
                    targetText = "How do you say this naturally?",
                    pronunciation = "হাউ ডু ইউ সে দিস ন্যাচারালি?",
                    nativeMeaning = if (isBn) "এটি স্বাভাবিকভাবে কীভাবে বলা যায়?" else "Ask for a natural phrasing."
                )
            )

            LearningModule.GRAMMAR -> grammarLessons.first().phrases

            LearningModule.VOCABULARY -> when (level) {
                LanguageLevel.BEGINNER, LanguageLevel.ELEMENTARY -> listOf(
                    VocabPhrase("Grateful", "গ্রেটফুল · /ˈɡreɪtfəl/", if (isBn) "কৃতজ্ঞ (I am grateful for your help.)" else "Feeling thankful and appreciative."),
                    VocabPhrase("Curious", "কিউরিয়াস · /ˈkjʊəriəs/", if (isBn) "কৌতূহলী (I'm curious to learn more.)" else "Eager to learn or know something."),
                    VocabPhrase("Reliable", "রিলায়েবল · /rɪˈlaɪəbl/", if (isBn) "নির্ভরযোগ্য (She is a reliable friend.)" else "Someone or something you can trust."),
                    VocabPhrase("Convenient", "কনভিনিয়েন্ট · /kənˈviːniənt/", if (isBn) "সুবিধাজনক (Is tomorrow convenient for you?)" else "Easy to use or fitting your schedule.")
                )
                else -> listOf(
                    VocabPhrase("Resilient", "রিজিলিয়েন্ট · /rɪˈzɪliənt/", if (isBn) "সহনশীল ও ঘুরে দাঁড়াতে সক্ষম।" else "Able to recover quickly from challenges."),
                    VocabPhrase("Articulate", "আর্টিকুলেট · /ɑːrˈtɪkjʊlət/", if (isBn) "স্পষ্ট ও প্রাঞ্জলভাবে কথা বলতে সক্ষম।" else "Expressing ideas clearly and fluently."),
                    VocabPhrase("Pragmatic", "প্র্যাগম্যাটিক · /præɡˈmætɪk/", if (isBn) "বাস্তববাদী ও কার্যকর দৃষ্টিভঙ্গিসম্পন্ন।" else "Practical and focused on real results."),
                    VocabPhrase("Breakthrough", "ব্রেকথ্রু · /ˈbreɪkθruː/", if (isBn) "বড় সাফল্য বা যুগান্তকারী অগ্রগতি।" else "A sudden important discovery or progress.")
                )
            }

            LearningModule.PRONUNCIATION -> listOf(
                VocabPhrase("Comfortable", "কাম্ফ-টা-বল · /ˈkʌmftəbəl/", if (isBn) "'কম-ফোর-টেবল' নয়, সংক্ষেপে 'কাম্ফটাবল' বলুন।" else "3 syllables: KUMF-tuh-bul (not 4)."),
                VocabPhrase("Wednesday", "ওয়েনজ-ডে · /ˈwenzdeɪ/", if (isBn) "মাঝের 'd' নীরব থাকে — 'ওয়েনজডে' উচ্চারণ করুন।" else "Silent 'd': pronounce as WENZ-day."),
                VocabPhrase("Schedule", "স্কেজুল · /ˈskedʒuːl/", if (isBn) "আমেরিকান: স্কেজুল, ব্রিটিশ: শেডিউল — দুটোই সঠিক।" else "US: SKEJ-ool, UK: SHED-yool."),
                VocabPhrase("Often", "অফেন · /ˈɔːfən/", if (isBn) "স্বাভাবিক কথোপকথনে 't' নরম রেখে 'অফেন' বলুন।" else "Usually pronounced OFF-en with a soft or silent 't'.")
            )

            LearningModule.QUIZ -> emptyList()

            LearningModule.OFFICE -> listOf(
                VocabPhrase("Thank you for taking the time to meet with me.", "থ্যাংক ইউ ফর টেকিং দ্য টাইম টু মিট উইথ মি।", if (isBn) "আমার সাথে দেখা করার সময় দেওয়ার জন্য ধন্যবাদ। (ইন্টারভিউ ও মিটিং)" else "Polite opening for interviews and meetings."),
                VocabPhrase("Could you share an update on the project?", "কুড ইউ শেয়ার অ্যান আপডেট অন দ্য প্রজেক্ট?", if (isBn) "প্রজেক্টের সর্বশেষ অবস্থাটা একটু জানাবেন?" else "Professional way to ask about progress."),
                VocabPhrase("Let me follow up with you by email.", "লেট মি ফলো আপ উইথ ইউ বাই ইমেইল।", if (isBn) "আমি ইমেইলের মাধ্যমে আপনাকে বিস্তারিত জানাব।" else "Promise a written follow-up after a call."),
                VocabPhrase("My biggest strength is solving problems calmly.", "মাই বিগেস্ট স্ট্রেংথ ইজ সলভিং প্রবলেমস কামলি।", if (isBn) "আমার সবচেয়ে বড় শক্তি হলো শান্তভাবে সমস্যার সমাধান করা।" else "Strong interview answer about your strengths.")
            )

            LearningModule.TRAVEL -> listOf(
                VocabPhrase("Could I have a window seat, please?", "কুড আই হ্যাভ আ উইন্ডো সিট, প্লিজ?", if (isBn) "আমি কি জানালার পাশের একটি সিট পেতে পারি?" else "Request a window seat at airport check-in."),
                VocabPhrase("How do I get to the city center from here?", "হাউ ডু আই গেট টু দ্য সিটি সেন্টার ফ্রম হিয়ার?", if (isBn) "এখান থেকে শহরের কেন্দ্রস্থলে কীভাবে যাব?" else "Ask for transit directions anywhere."),
                VocabPhrase("What local dish do you recommend?", "হোয়াট লোকাল ডিশ ডু ইউ রেকমেন্ড?", if (isBn) "এখানকার কোন স্থানীয় খাবারটি আপনি সুপারিশ করবেন?" else "Order great local food at a restaurant."),
                VocabPhrase("Could we get the check, please?", "কুড উই গেট দ্য চেক, প্লিজ?", if (isBn) "দয়া করে বিলটি দেবেন?" else "Politely ask for the bill after dining.")
            )
        }
    }

    fun progressiveLessonPhrases(
        targetCode: String,
        nativeCode: String,
        level: LanguageLevel,
        topic: String
    ): List<VocabPhrase> {
        val isBn = nativeCode == "bn"
        return when (level) {
            LanguageLevel.BEGINNER -> listOf(
                VocabPhrase(
                    targetText = t(targetCode, "Hello, how are you?", "Hola, ¿cómo estás?", "Bonjour, comment allez-vous ?", "Hallo, wie geht es Ihnen?", "مرحباً، كيف حالك؟", "こんにちは、お元気ですか？", "안녕하세요, 잘 지내세요?", "नमस्ते, आप कैसे हैं?", "你好，你怎么样？", "Ciao, come stai?", "Olá, tudo bem?", "হ্যালো, কেমন আছেন?", "Merhaba, nasılsınız?", "Привет, как дела?"),
                    pronunciation = p(targetCode, "হ্যালো, হাউ আর ইউ? · /həˈloʊ haʊ ɑːr juː/", "ওলা, কোমো এস্তাস?", "বঁজুর, কমঁ তালে ভু?", "হালো, ভি গেট এস ইনেন?", "মারহাবান, কাইফা হালুক?", "কোন্নিচিওয়া, ওগেনকি দেস্কা?", "আন্নিয়ংহাসিও, জাল জিনেসেয়ো?"),
                    nativeMeaning = if (isBn) "হ্যালো, আপনি কেমন আছেন? — কথা শুরু করার জন্য।" else "Warm greeting to start any conversation."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Good morning!", "Buenos días", "Bonjour", "Guten Morgen", "صباح الخير", "おはようございます", "좋은 아침이에요", "सुप्रभात", "早上好", "Buongiorno", "Bom dia", "সুপ্রভাত", "Günaydın", "Доброе утро"),
                    pronunciation = p(targetCode, "গুড মর্নিং · /ɡʊd ˈmɔːrnɪŋ/", "বুয়েনোস দিয়াস", "বঁজুর", "গুটেন মর্গেন", "সাবাহ আল-খাইর", "ওহাইয়ো গোজাইমাসু", "জোউন আছিমিয়েয়ো"),
                    nativeMeaning = if (isBn) "সকালে শুভেচ্ছা জানাতে।" else "To greet someone in the morning."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "My name is...", "Me llamo...", "Je m'appelle...", "Ich heiße...", "اسمي...", "私の名前は...です", "제 이름은...입니다", "मेरा नाम ... है", "我叫...", "Mi chiamo...", "Meu nome é...", "আমার নাম...", "Benim adım...", "Меня зовут..."),
                    pronunciation = p(targetCode, "মাই নেম ইজ... · /maɪ neɪm ɪz/", "মে ইয়ামো", "জ্য মাপেল", "ইশ হাইসে", "ইসমি...", "ওয়াতাশি নো নামায়ে ওয়া...", "জে ইরুমুন..."),
                    nativeMeaning = if (isBn) "নিজের নাম পরিচয় দিতে।" else "To introduce your own name."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Nice to meet you!", "¡Mucho gusto!", "Enchanté !", "Freut mich!", "تشرفت بمعرفتك!", "はじめまして！", "만나서 반가워요!", "आपसे मिलकर खुशी हुई!", "很高兴认识你！", "Piacere di conoscerti!", "Prazer em conhecer você!", "আপনার সাথে পরিচিত হয়ে ভালো লাগল!", "Tanıştığımıza memnun oldum!", "Приятно познакомиться!"),
                    pronunciation = p(targetCode, "নাইস টু মিট ইউ! · /naɪs tuː miːt juː/", "মুচো গুস্তো!", "অঁশঁতে!", "ফ্রয়েট মিশ!", "তাশাররাফতু...", "হাজিমেমাশিতে!", "মান্নাসো বানগাওয়য়ো!"),
                    nativeMeaning = if (isBn) "কারো সাথে প্রথম পরিচয়ে ভদ্রভাবে বলতে।" else "Polite phrase when meeting someone new."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "Thank you very much!", "¡Muchas gracias!", "Merci beaucoup !", "Vielen Dank!", "شكراً جزيلاً!", "ありがとうございます！", "정말 감사합니다!", "बहुत धन्यवाद!", "非常感谢！", "Grazie mille!", "Muito obrigado!", "আপনাকে অনেক ধন্যবাদ!", "Çok teşekkürler!", "Большое спасибо!"),
                    pronunciation = p(targetCode, "থ্যাংক ইউ ভেরি মাচ! · /θæŋk juː ˈveri mʌtʃ/", "মুচাস গ্রাসিয়াস!", "মের্সি বোকু!", "ফিলেন ডাংক!", "শুকরান জাজিলান!", "আরিগাতো গোজাইমাসু!", "জংমাল গামসাহামনিদা!"),
                    nativeMeaning = if (isBn) "আন্তরিক ধন্যবাদ জানাতে।" else "Express warm gratitude."
                )
            )

            LanguageLevel.ELEMENTARY -> listOf(
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
                    targetText = t(targetCode, "What do you do in your free time?", "¿Qué haces en tu tiempo libre?", "Que faites-vous pendant votre temps libre ?", "Was machen Sie in Ihrer Freizeit?", "ماذا تفعل في وقت فراغك؟", "暇な時は何をしますか？", "여가 시간에 주로 무엇을 해요?", "आप अपने खाली समय में क्या करते हैं?", "你空闲时间喜欢做什么？", "Cosa fai nel tempo libero?", "O que você faz no tempo livre?", "অবসর সময়ে আপনি কী করেন?", "Boş zamanlarınızda ne yaparsınız?", "Чем вы занимаетесь в свободное время?"),
                    pronunciation = p(targetCode, "হোয়াট ডু ইউ ডু ইন ইয়োর ফ্রি টাইম?", "কে আসেস এন তু তিয়েম্পো লিব্রে?", "ক্য ফেত ভু...", "ভাস মাখেন জি...", "মাজা তাফআল...", "হিমা না তোকি...", "ইয়োগা শিগানে..."),
                    nativeMeaning = if (isBn) "কারো শখ বা অবসর সময়ের কাজ জানতে চাইলে।" else "Ask someone about their hobbies."
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
                    nativeMeaning = if (isBn) "সিদ্ধান্ত নেওয়ার আগে সুবিধা-অসুবিধা বিচার করতে।" else "Discuss trade-offs before deciding."
                ),
                VocabPhrase(
                    targetText = t(targetCode, "I couldn't agree with you more.", "No podría estar más de acuerdo contigo.", "Je ne pourrais pas être plus d'accord.", "Da kann ich Ihnen nur voll zustimmen.", "أتفق معك تماماً.", "これ以上ないほど賛成です。", "전적으로 동의합니다.", "मैं आपसे पूरी तरह सहमत हूँ।", "我完全同意你的看法。", "Non potrei essere più d'accordo.", "Não poderia concordar mais com você.", "আমি আপনার সাথে শতভাগ একমত।", "Size kesinlikle katılıyorum.", "Я полностью с вами согласен."),
                    pronunciation = p(targetCode, "আই কুডন্ট অ্যাগ্রি উইথ ইউ মোর।", "নো পোদরিয়া এস্তার মাস...", "জ্য ন পুরে পা এত্র...", "ডা কান ইশ...", "আত্তাফিকু মা'আকা তামামান", "কোরে ইজো নাই...", "জনজগুরো দোংউইহামনিদা"),
                    nativeMeaning = if (isBn) "কারো কথার সাথে জোরালোভাবে একমত হতে।" else "Express strong, natural agreement."
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
                    targetText = t(targetCode, "I'd truly appreciate your candid feedback on this.", "Agradecería mucho tus comentarios sinceros sobre esto.", "J'apprécierais vraiment votre retour franc à ce sujet.", "Ich wäre für Ihr offenes Feedback sehr dankbar.", "سأقدر حقاً ملاحظاتك الصريحة حول هذا.", "これについて率直なご意見をいただければ幸いです。", "이에 대한 솔직한 피드백을 주시면 감사하겠습니다.", "मैं इस पर आपकी स्पष्ट प्रतिक्रिया की सराहना करूँगा।", "我非常感谢您对此提出坦诚的反馈。", "Apprezzerei davvero il tuo feedback sincero.", "Eu realmente agradeceria seu feedback sincero.", "এই বিষয়ে আপনার খোলামেলা ও সৎ মতামত পেলে আমি সত্যিই কৃতজ্ঞ থাকব।", "Bu konudaki samimi geri bildiriminize çok değer veririm.", "Я буду искренне признателен за ваш честный отзыв."),
                    pronunciation = p(targetCode, "আইড ট্রুলি অ্যাপ্রিশিয়েট ইয়োর ক্যান্ডিড ফিডব্যাক।", "আগ্রাদেসেরিয়া মুচো...", "জাপ্রেসিয়ারে ভ্রেমঁ...", "ইশ ভ্যারে ফিউর...", "সা'উকাদ্দিরু হাক্কান...", "সোচ্চোকু না গোইকেন...", "সোলজিকান ফিদুবেগুল..."),
                    nativeMeaning = if (isBn) "পেশাদার ও মার্জিত ভঙ্গিতে সৎ মতামত চাইতে।" else "Invite honest feedback with poise."
                )
            )
        }
    }

    fun rolePlayOpeningLine(targetCode: String, scenarioId: String): String = when (scenarioId) {
        "cafe_cooking" -> t(targetCode, "Welcome in! Our espresso and cinnamon croissants are fresh out of the oven. What can I make for you today?", "¡Bienvenido! Nuestro espresso y croissants están recién salidos del horno. ¿Qué le preparo hoy?", "Bienvenue ! Nos croissants sortent du four. Que puis-je vous servir aujourd'hui ?", "Willkommen! Unser Kaffee und frisches Gebäck sind bereit. Was darf es heute sein?", "أهلاً بك! القهوة والكرواسون طازجان الآن. ماذا تحب أن تطلب اليوم؟", "いらっしゃいませ！焼きたてのクロワッサンとコーヒーはいかがですか？", "어서 오세요! 갓 구운 크루아상과 커피가 준비되어 있어요. 무엇을 드릴까요?")
        "airport_travel" -> t(targetCode, "Good afternoon! Welcome to check-in. May I see your passport and booking confirmation, please?", "¡Buenas tardes! Bienvenido al mostrador. ¿Puedo ver su pasaporte y reserva?", "Bonjour ! Bienvenue à l'enregistrement. Puis-je voir votre passeport ?", "Guten Tag! Willkommen beim Check-in. Darf ich Ihren Reisepass sehen?", "مساء الخير! مرحباً بك في تسجيل الوصول. هل يمكنني رؤية جواز سفرك؟", "こんにちは！チェックインへようこそ。パスポートを拝見できますか？", "안녕하세요! 체크인 카운터입니다. 여권과 예약 번호를 보여주시겠어요?")
        "job_interview" -> t(targetCode, "Thanks for joining us today! To start off, could you tell me about yourself and what excites you about this role?", "¡Gracias por venir hoy! Para empezar, ¿podrías hablarme de ti y qué te entusiasma de este puesto?", "Merci d'être venu ! Pouvez-vous vous présenter et dire ce qui vous motive pour ce poste ?", "Danke, dass Sie heute hier sind! Erzählen Sie mir kurz über sich und Ihre Motivation.", "شكراً لانضمامك إلينا اليوم! هل يمكنك التحدث قليلاً عن نفسك؟", "本日はありがとうございます！まず自己紹介と志望動機を教えていただけますか？", "오늘 와주셔서 감사합니다! 먼저 간단한 자기소개를 부탁드릴게요.")
        else -> t(targetCode, "Hello and welcome to our boutique! Are you looking for something special today?", "¡Hola y bienvenido a nuestra tienda! ¿Busca algo especial hoy?", "Bonjour et bienvenue dans notre boutique ! Cherchez-vous quelque chose de spécial ?", "Hallo und willkommen! Suchen Sie heute etwas Bestimmtes?", "مرحباً بك في متجرنا! هل تبحث عن شيء مميز اليوم؟", "いらっしゃいませ！今日は何かお探しですか？", "안녕하세요! 오늘 특별히 찾으시는 물건이 있으신가요?")
    }

    fun rolePlayStarterPhrases(targetCode: String, nativeCode: String, scenarioId: String): List<VocabPhrase> {
        val isBn = nativeCode == "bn"
        return when (scenarioId) {
            "cafe_cooking" -> listOf(
                VocabPhrase(t(targetCode, "I'd love an iced latte with oat milk, please.", "Me encantaría un café con leche de avena frío.", "Je voudrais un latte glacé au lait d'avoine.", "Ich hätte gern einen Eislatte mit Hafermilch.", "أريد لاتيه مثلج بحليب الشوفان من فضلك.", "オーツミルクのアイスラテをお願いします。", "오트 밀크 아이스 라떼 주세요."), "", if (isBn) "ভদ্রভাবে কফি অর্ডার করতে।" else "Politely order a customized coffee."),
                VocabPhrase(t(targetCode, "What spices do you use in this pastry?", "¿Qué especias usan en este pastel?", "Quelles épices utilisez-vous dans cette pâtisserie ?", "Welche Gewürze verwenden Sie hier?", "ما التوابل المستخدمة في هذه المعجنات؟", "このお菓子にはどんなスパイスを使っていますか？", "이 빵에는 어떤 향신료가 들어가나요?"), "", if (isBn) "উপাদান ও স্বাদ সম্পর্কে জানতে চাইলে।" else "Ask about ingredients and flavors.")
            )
            "airport_travel" -> listOf(
                VocabPhrase(t(targetCode, "Here is my passport. Could I get a window seat?", "Aquí está mi pasaporte. ¿Podría darme asiento de ventanilla?", "Voici mon passeport. Pourrais-je avoir un siège côté hublot ?", "Hier ist mein Pass. Hätte ich bitte einen Fensterplatz?", "هذا جواز سفري. هل يمكنني الحصول على مقعد بجانب النافذة؟", "パスポートです。窓側の席をお願いできますか？", "여기 여권입니다. 창가 자리로 부탁드려요."), "", if (isBn) "পাসপোর্ট দিয়ে জানালার পাশের সিট চাইতে।" else "Present passport and request a window seat."),
                VocabPhrase(t(targetCode, "Which gate does my flight depart from?", "¿De qué puerta sale mi vuelo?", "De quelle porte part mon vol ?", "Von welchem Gate fliegt mein Flug ab?", "من أي بوابة تغادر رحلتي؟", "搭乗ゲートは何番ですか？", "탑승 게이트가 어디인가요?"), "", if (isBn) "বোর্ডিং গেট নম্বর নিশ্চিত করতে।" else "Confirm your boarding gate.")
            )
            "job_interview" -> listOf(
                VocabPhrase(t(targetCode, "I specialize in building user-focused mobile apps.", "Me especializo en crear aplicaciones móviles centradas en el usuario.", "Je me spécialise dans les applications mobiles centrées sur l'utilisateur.", "Ich spezialisiere mich auf nutzerfreundliche Apps.", "أتخصص في تطوير التطبيقات التي تركز على المستخدم.", "ユーザー中心のアプリ開発を専門としています。", "사용자 중심의 앱 개발을 전문으로 합니다."), "", if (isBn) "নিজের দক্ষতা ও অভিজ্ঞতা জানাতে।" else "Highlight your core expertise."),
                VocabPhrase(t(targetCode, "What does success look like in the first 90 days?", "¿Cómo se ve el éxito en los primeros 90 días?", "À quoi ressemble la réussite dans les 90 premiers jours ?", "Wie sieht Erfolg in den ersten 90 Tagen aus?", "كيف يبدو النجاح في أول 90 يوماً؟", "最初の90日間で期待される成果は何ですか？", "입사 후 첫 90일 동안 기대하는 성과는 무엇인가요?"), "", if (isBn) "চাকরিদাতাকে স্মার্ট প্রশ্ন করতে।" else "Ask a thoughtful question to the interviewer.")
            )
            else -> listOf(
                VocabPhrase(t(targetCode, "Do you have this jacket in medium?", "¿Tiene esta chaqueta en talla mediana?", "Avez-vous cette veste en taille M ?", "Haben Sie diese Jacke in Größe M?", "هل لديكم هذه السترة بمقاس متوسط؟", "このジャケットのMサイズはありますか？", "이 재킷 미디엄 사이즈 있나요?"), "", if (isBn) "ভিন্ন সাইজ বা রঙের খোঁজ নিতে।" else "Ask for a different size."),
                VocabPhrase(t(targetCode, "Could you offer a discount if I buy two?", "¿Podría hacer un descuento si llevo dos?", "Pourriez-vous faire une remise si j'en prends deux ?", "Gibt es einen Rabatt, wenn ich zwei kaufe?", "هل يمكن تقديم خصم إذا اشتريت اثنتين؟", "2つ買うと割引になりますか？", "두 개 사면 할인해 주실 수 있나요?"), "", if (isBn) "ভদ্রভাবে ছাড়ের কথা বলতে।" else "Politely ask for a bundle discount.")
            )
        }
    }

    fun t(
        code: String,
        en: String,
        es: String = en,
        fr: String = en,
        de: String = en,
        ar: String = en,
        ja: String = en,
        ko: String = en,
        hi: String = en,
        zh: String = en,
        it: String = en,
        pt: String = en,
        bn: String = en,
        tr: String = en,
        ru: String = en
    ): String = when (code.lowercase()) {
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

    fun p(
        code: String,
        en: String,
        es: String = "",
        fr: String = "",
        de: String = "",
        ar: String = "",
        ja: String = "",
        ko: String = ""
    ): String = when (code.lowercase()) {
        "en" -> en
        "es" -> es
        "fr" -> fr
        "de" -> de
        "ar" -> ar
        "ja" -> ja
        "ko" -> ko
        else -> ""
    }
}
