package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
interface Sanitizer {
    void sanitize(StringResource resource);
    void rebuild(TranslationResult result);
}