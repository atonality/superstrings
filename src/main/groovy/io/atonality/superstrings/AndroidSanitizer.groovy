package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
class AndroidSanitizer extends JavaSanitizer {

    @Override
    String sanitize(StringResource resource) {
        String value = super.sanitize(resource)
        boolean cdata = resource.metadata['cdata'] as Boolean
        if (!cdata) {
            value = value.replace("\\'", "'")
            value = value.replace('\\"', '"')
        }
        return value
    }

    @Override
    String rebuild(TranslationResult result) {
        String value = super.rebuild(result)
        boolean cdata = result.resource.metadata['cdata'] as Boolean
        if (cdata) {
            value = "<![CDATA[${value}]]>"
        } else {
            value = value.replace("'", "\\'")
            value = value.replace('"', '\\"')
        }
        return value
    }
}