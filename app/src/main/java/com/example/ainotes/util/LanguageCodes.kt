package com.example.ainotes.util

object LanguageCodes {
    // Display name -> BCP-47 tag used by SpeechRecognizer
    val displayToTag: Map<String, String> = mapOf(
        // English
        "English (US)" to "en-US",
        "English (UK)" to "en-GB",
        "English (India)" to "en-IN",

        // Spanish
        "Spanish" to "es-ES",          // generic choice → Spain
        "Spanish (Spain)" to "es-ES",
        "Spanish (US)" to "es-US",
        "Spanish (Mexico)" to "es-MX",

        // Popular EU languages
        "French" to "fr-FR",
        "German" to "de-DE",
        "Italian" to "it-IT",
        "Portuguese (Brazil)" to "pt-BR",
        "Portuguese (Portugal)" to "pt-PT",
        "Dutch" to "nl-NL",
        "Swedish" to "sv-SE",
        "Danish" to "da-DK",
        "Norwegian" to "no-NO",
        "Finnish" to "fi-FI",
        "Polish" to "pl-PL",
        "Czech" to "cs-CZ",

        // Asia
        "Japanese" to "ja-JP",
        "Korean" to "ko-KR",
        "Chinese (Simplified)" to "zh-CN",
        "Chinese (Traditional)" to "zh-TW",
        "Thai" to "th-TH",
        "Vietnamese" to "vi-VN",
        "Indonesian" to "id-ID",

        // Others
        "Russian" to "ru-RU",
        "Turkish" to "tr-TR",
        "Ukrainian" to "uk-UA",
        "Hebrew" to "he-IL",
        "Arabic (Saudi Arabia)" to "ar-SA",
        "Hindi" to "hi-IN"
    )

    fun nameToTag(name: String): String =
        displayToTag[name] ?: displayToTag["English (US)"]!!
}
