package io.atonality.superstrings

import groovy.json.JsonSlurper
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

// TODO: handle quota of 10,000 characters / 100 seconds
class GoogleTranslator implements Translator {

    static final double COST_PER_CHARACTER = 20.0 / 1000000
    static final String API_ENDPOINT = "https://www.googleapis.com/language/translate/v2"

    String apiKey
    Language sourceLanguage
    OkHttpClient client

    public GoogleTranslator(String apiKey, Language sourceLanguage) {
        this.apiKey = apiKey
        this.sourceLanguage = sourceLanguage
        this.client = new OkHttpClient.Builder().build()
    }

    @Override
    double getEstimatedCost(List<Translation> translations) {
        long characters = translations.sum { Translation item ->
            item.resource.value.length()
        } as long
        return characters * COST_PER_CHARACTER
    }

    @Override
    TranslationResult translate(Translation translation) {
        def url = HttpUrl.parse(API_ENDPOINT).newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("source", sourceLanguage.googleCode)
                .addQueryParameter("target", translation.targetLanguage.googleCode)
                .addQueryParameter("q", translation.resource.value)
                .build()
        def request = new Request.Builder().url(url).build()
        def response = client.newCall(request).execute()

        def json = new JsonSlurper().parseText(response.body().string())
        def translations = json?.data?.translations as List
        def translatedValue = translations?.get(0)?.translatedText as String
        if (!translatedValue) {
            throw new IOException("Unable to parse translatedText field from result JSON")
        }
        return new TranslationResult(language: translation.targetLanguage,
                translatedValue: translatedValue,
                dateTranslated: new Date())
    }
}