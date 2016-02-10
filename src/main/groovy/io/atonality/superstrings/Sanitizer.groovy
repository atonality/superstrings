package io.atonality.superstrings

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class Sanitizer {

    String sanitize(StringResource resource) {
        String value = resource.value

        def arguments = getPositionalArguments(resource)
        value = sanitizePositionalArguments(resource, value, arguments)

        return value
    }

    List<String> getPositionalArguments(StringResource resource) {
        return []
    }

    String sanitizePositionalArguments(StringResource resource, String value, List<String> arguments) {
        // intellij is showing an unnecessary error when using eachWithIndex; therefore,
        // a regular for loop is used here
        for (int i = 0; i < arguments.size(); i++) {
            def arg = arguments[i]

            int pos = value.indexOf(arg)
            def prefix = value.substring(0, pos)
            def suffix = value.substring(pos + arg.length())

            value = "${prefix}${buildPositionalSpecifier(i)}${suffix}"
        }
        resource.metadata['args'] = arguments
        return value
    }

    String rebuild(TranslationResult result) {
        def value = rebuildPositionalArguments(result.resource, result.translatedValue)
        return value
    }

    String rebuildPositionalArguments(StringResource resource, String value) {
        def arguments = resource.metadata['args'] as List<String>

        // intellij is showing an unnecessary error when using eachWithIndex; therefore,
        // a regular for loop is used here
        for (int i = 0; i < arguments.size(); i++) {
            def arg = arguments[i]

            def pattern = Pattern.compile(buildPositionalSpecifierRegex(i))
            String specifier = value.find(pattern)
            if (!specifier) {
                println "WARNING: Failed to find positional specifier ${pattern.toString()} in translated string: ${value}"
                continue
            }
            int pos = value.indexOf(specifier)
            def prefix = value.substring(0, pos)
            def suffix = value.substring(pos + specifier.length())

            value = "${prefix}${arg}${suffix}"
        }
        return value
    }

    protected String buildPositionalSpecifier(int index) {
        "#${index + 1}"
    }

    protected String buildPositionalSpecifierRegex(int index) {
        "#\\s*${index + 1}"
    }
}