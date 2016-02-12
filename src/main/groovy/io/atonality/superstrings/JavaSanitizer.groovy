package io.atonality.superstrings

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class JavaSanitizer extends Sanitizer {

    // from java.util.Formatter.class
    // %[argument_index$][flags][width][.precision][t]conversion
    static final String FMT_SPECIFIER_REGEX = '%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])'

    // http://stackoverflow.com/questions/1367322/what-are-all-the-escape-characters-in-java
    // TODO: add additional escape sequences
    static final String ESCAPE_SEQUENCE_REGEX = "(\\\\[btnfr])+"

    Pattern fmtSpecifierPattern
    Pattern escapeSequencePattern

    JavaSanitizer(SuperstringsMetadata metadata) {
        super(metadata)
        fmtSpecifierPattern = Pattern.compile(FMT_SPECIFIER_REGEX)
        escapeSequencePattern = Pattern.compile(ESCAPE_SEQUENCE_REGEX)
    }

    @Override
    List<String> getPositionalArguments(StringResource resource) {
        def args = super.getPositionalArguments(resource)

        def fmtSpecifiers = resource.value.findAll(fmtSpecifierPattern)
        args += fmtSpecifiers

        def escapeSequences = resource.value.findAll(escapeSequencePattern)
        args += escapeSequences

        return args
    }
}