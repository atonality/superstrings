package io.atonality.superstrings

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.Listing
import groovy.transform.CompileStatic

@CompileStatic
class GooglePlayOutput implements Output {

    final String packageName
    final AndroidPublisher service

    String editId
    String untranslatedAppTitle

    GooglePlayOutput(final String packageName, final String serviceAccountId, final File privateKeyFile) {
        this.packageName = packageName
        if (!privateKeyFile?.exists() && privateKeyFile.canRead()) {
            throw new IOException("Unable to read service account private key file: ${privateKeyFile?.absolutePath}")
        }
        final transport = GoogleNetHttpTransport.newTrustedTransport()
        final jsonFactory = JacksonFactory.getDefaultInstance()

        final credential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(serviceAccountId)
                .setServiceAccountPrivateKeyFromP12File(privateKeyFile)
                .setServiceAccountScopes(AndroidPublisherScopes.all())
                .build()
        service = new AndroidPublisher.Builder(transport, jsonFactory, credential)
                .setApplicationName('Superstrings')
                .build()
    }

    @Override
    File getCacheFile() {
        return null
    }

    @Override
    void onResourcesParsed(List<StringResource> resources) {
        untranslatedAppTitle = resources.find { StringResource resource ->
            resource.id == GooglePlayFileParser.APP_TITLE_ID
        }?.value
    }

    @Override
    String outputTranslations(Set<StringResource> translatedResources, Language targetLanguage) {
        if (!editId) {
            final edit = service.edits().insert(packageName, null).execute()
            editId = edit.getId()
            println "Created edit with id: ${editId}"
        }
        final appTitleTranslation = findTranslationWithId(translatedResources,
                GooglePlayFileParser.APP_TITLE_ID, targetLanguage)
        String appTitle = appTitleTranslation?.translatedValue ?: untranslatedAppTitle

        final shortDescription = findTranslationWithId(translatedResources,
                GooglePlayFileParser.SHORT_DESCRIPTION_ID, targetLanguage)?.translatedValue
        if (!shortDescription) {
            println "Failed to find short description for language: ${targetLanguage.toString()}"
            return
        }
        final fullDescription = findTranslationWithId(translatedResources,
                GooglePlayFileParser.FULL_DESCRIPTION_ID, targetLanguage)?.translatedValue
        if (!fullDescription) {
            println "Failed to find full description for language: ${targetLanguage.toString()}"
            return
        }
        final languageCode = targetLanguage.googlePlayCode
        println "Updating Google Play listing for language: ${targetLanguage.toString()}"

        final listing = new Listing()
        listing.setTitle(appTitle)
        listing.setShortDescription(shortDescription)
        listing.setFullDescription(fullDescription)

        final update = service.edits().listings().update(packageName, editId, languageCode, listing)
        update.execute()
        return "Submitted listing update for language: ${targetLanguage.toString()}"
    }

    protected TranslationResult findTranslationWithId(Set<StringResource> translatedResources, String id, Language targetLanguage) {
        translatedResources.find() { StringResource resource ->
            resource.id == id
        }?.translations?.find { it.language == targetLanguage }
    }

    @Override
    void finish() {
        println "Committing edit"

        final commit = service.edits().commit(packageName, editId)
        final commitResult = commit.execute()
        println "Committed edit successfully: ${commitResult.getId()}"
    }
}