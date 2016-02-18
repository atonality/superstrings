package io.atonality.superstrings

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class AndroidSanitizer extends JavaSanitizer {

    static final String SINGLE_QUOTE_REGEX = '\'|&#39;'
    static final String DOUBLE_QUOTE_REGEX = '"|&#34;'

    final Pattern singleQuotePattern
    final Pattern doubleQuotePattern

    def AndroidSanitizer(SuperstringsMetadata metadata) {
        super(metadata)
        singleQuotePattern = Pattern.compile(SINGLE_QUOTE_REGEX)
        doubleQuotePattern = Pattern.compile(DOUBLE_QUOTE_REGEX)
    }

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
    String rebuild(TranslationResult result) throws IOException {
        String value = super.rebuild(result)
        boolean cdata = result.resource.metadata['cdata'] as Boolean
        if (cdata) {
            value = "<![CDATA[${value}]]>"
        } else {
            value.findAll(singleQuotePattern).each { String match ->
                value = value.replace(match, "\\'")
            }
            value.findAll(doubleQuotePattern).each { String match ->
                value = value.replace(match, '\\"')
            }
        }
        value = value.replaceAll('\\.\\.\\.', '…')
        return value
    }
}