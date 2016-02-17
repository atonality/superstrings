package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
interface Output {

    File getCacheFile()
    void onResourcesParsed(List<StringResource> resources)
    String outputTranslations(Set<StringResource> translatedResources, Language targetLanguage)
    void finish()
}