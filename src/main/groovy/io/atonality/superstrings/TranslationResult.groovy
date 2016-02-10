package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
class TranslationResult {

    StringResource resource
    Language language
    String translatedValue
    Date dateTranslated
}