package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
class StringResource {

    String id
    String value
    boolean translatable
    List<TranslationResult> translations = []
}