package io.atonality.superstrings

import groovy.json.JsonSlurper
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class GoogleTranslator implements Translator {

    static final String API_ENDPOINT = "https://www.googleapis.com/language/translate/v2"

    static final int QUOTA_CHARACTER_COUNT = 10000
    static final long QUOTA_TIMEOUT_MS = 100L * 1000L
    static final double COST_PER_CHARACTER = 20.0 / 1000000

    String apiKey
    Language sourceLanguage
    OkHttpClient client
    Set<TranslationResult> results

    public GoogleTranslator(String apiKey, Language sourceLanguage) {
        this.apiKey = apiKey
        this.sourceLanguage = sourceLanguage
        this.client = new OkHttpClient.Builder().build()
        this.results = []
    }

    @Override
    double getEstimatedCost(List<Translation> translations) {
        if (translations?.isEmpty()) {
            return 0
        }
        long characters = translations.sum { Translation item ->
            item.resource.value.length()
        } as long
        return characters * COST_PER_CHARACTER
    }

    @Override
    TranslationResult translate(Translation translation) {
        while (checkQuotaExceeded()) {
            long seconds = QUOTA_TIMEOUT_MS / 1000L
            println("Quota exceeded (${QUOTA_CHARACTER_COUNT} characters per ${seconds} seconds)")
            println("Sleeping for 5 seconds...\n")
            sleep(5000)
        }
        def url = HttpUrl.parse(API_ENDPOINT).newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("source", sourceLanguage.isoCode)
                .addQueryParameter("target", translation.targetLanguage.isoCode)
                .addQueryParameter("q", translation.resource.sanitizedValue)
                .build()
        def request = new Request.Builder().url(url).build()
        def response = client.newCall(request).execute()

        def json = new JsonSlurper().parseText(response.body().string())
        def translations = json?.data?.translations as List
        def translatedValue = translations?.get(0)?.translatedText as String
        if (!translatedValue) {
            throw new IOException("Unable to parse translatedText field from result JSON")
        }
        def result = new TranslationResult(language: translation.targetLanguage,
                translatedValue: translatedValue,
                dateTranslated: new Date(),
                resource: translation.resource)
        results << result
        return result
    }

    protected boolean checkQuotaExceeded() {
        def relevantTranslations = results.findAll { TranslationResult result ->
            long elapsed = System.currentTimeMillis() - result.dateTranslated.time
            return elapsed < QUOTA_TIMEOUT_MS
        }
        if (relevantTranslations?.isEmpty()) {
            return false
        }
        long characterCount = relevantTranslations.sum { TranslationResult result ->
            result.resource.sanitizedValue.length()
        } as long
        return characterCount >= (QUOTA_CHARACTER_COUNT * 0.90)
    }
}