package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
class GooglePlaySanitizer extends Sanitizer {

    def GooglePlaySanitizer(SuperstringsMetadata metadata) {
        super(metadata)
    }

    @Override
    String sanitize(StringResource resource) {
        String value = super.sanitize(resource)
        value = value.replaceAll('[\\n\\r]', '<br/>')
        return value
    }
}
