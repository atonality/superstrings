package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
class AndroidSanitizer implements Sanitizer {

    @Override
    void sanitize(StringResource resource) {
        String value = resource.value
        boolean cdata = resource.metadata['cdata'] as Boolean
        if (!cdata) {
            value = resource.value.replace("\\'", "'")
            value = value.replace('\\"', '"')
        }
        resource.sanitizedValue = value
    }

    @Override
    void rebuild(TranslationResult result) {
        String value = result.translatedValue
        boolean cdata = result.resource.metadata['cdata'] as Boolean
        if (cdata) {
            value = "<![CDATA[${value}]]>"
        } else {
            value = result.translatedValue.replace("'", "\\'")
            value = value.replace('"', '\\"')
        }
        result.translatedValue = value
    }
}