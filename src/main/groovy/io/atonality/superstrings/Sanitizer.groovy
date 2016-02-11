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
        def result = [] as List<String>
        def properNames = (resource.metadata['properNames'] ?: []) as Set<String>
        properNames.each { String name ->
            def pattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE)
            result += resource.value.findAll(pattern)
        }
        return result
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

    String rebuild(TranslationResult result) throws IOException {
        def value = rebuildPositionalArguments(result.resource, result.translatedValue)
        return value
    }

    String rebuildPositionalArguments(StringResource resource, String value) throws IOException {
        def arguments = resource.metadata['args'] as List<String>

        // intellij is showing an unnecessary error when using eachWithIndex; therefore,
        // a regular for loop is used here
        for (int i = 0; i < arguments.size(); i++) {
            def arg = arguments[i]

            def pattern = Pattern.compile(buildPositionalSpecifierRegex(i))
            String specifier = value.find(pattern)
            if (!specifier) {
                throw new IOException("Failed to find positional specifier ${pattern.toString()} in translated string: ${value}")
            }
            int pos = value.indexOf(specifier)
            def prefix = value.substring(0, pos)
            def suffix = value.substring(pos + specifier.length())

            value = "${prefix}${arg}${suffix}"
        }
        return value
    }

    protected String buildPositionalSpecifier(int index) {
        "(^${index + 1})"
    }

    protected String buildPositionalSpecifierRegex(int index) {
        def leftBracket = '[\\(\\uff08]'
        def rightBracket = '[\\)\\uff09]'
        "${leftBracket}\\s*\\^\\s*${index + 1}\\s*${rightBracket}"
    }
}