package io.atonality.superstrings

import groovy.transform.CompileStatic

import java.util.regex.Pattern

// TODO: matching case of first character may fail if the first character is a "?" or other punctuation char
// TODO: replace w/ mapping does not replace w/ proper case
@CompileStatic
class Sanitizer {

    SuperstringsMetadata metadata

    Sanitizer(SuperstringsMetadata metadata) {
        this.metadata = metadata
    }

    String sanitize(StringResource resource) {
        String value = resource.value
        value = replaceWithMapping(value)

        def arguments = getPositionalArguments(resource)
        value = sanitizePositionalArguments(resource, value, arguments)

        return value
    }

    String replaceWithMapping(String value) {
        metadata.mapping.each { Map.Entry<String, String> entry ->
            def pattern = Pattern.compile("\\b${entry.key}\\b", Pattern.CASE_INSENSITIVE)
            value = value.replaceAll(pattern, entry.value)
        }
        return value
    }

    List<String> getPositionalArguments(StringResource resource) {
        def properNames = [] as Set<String>
        properNames += metadata.properNames
        properNames += (resource.metadata['properNames'] ?: []) as Set<String>

        def result = [] as List<String>
        properNames.each { String name ->
            def pattern = Pattern.compile("\\b${name}\\b", Pattern.CASE_INSENSITIVE)
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

        // update case of first character
        if (!value.isEmpty() && !result.resource.value.isEmpty()) {
            char pre = result.resource.value.charAt(0)
            char post = value.charAt(0)

            if (Character.isLetter(post) && Character.isUpperCase(pre) && !Character.isUpperCase(post)) {
                value = value.replaceFirst(post as String, post.toUpperCase() as String)
            } else if (Character.isLetter(post) && Character.isLowerCase(pre) && !Character.isLowerCase(pre)) {
                value = value.replaceFirst(post as String, post.toLowerCase() as String)
            }
        }
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