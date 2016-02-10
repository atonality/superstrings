package io.atonality.superstrings

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class JavaSanitizer extends Sanitizer {

    // from java.util.Formatter.class
    // %[argument_index$][flags][width][.precision][t]conversion
    static final String FMT_SPECIFIER_REGEX = '%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])'

    Pattern fmtSpecifierPattern

    JavaSanitizer() {
        fmtSpecifierPattern = Pattern.compile(FMT_SPECIFIER_REGEX)
    }

    @Override
    List<String> getPositionalArguments(StringResource resource) {
        def args = super.getPositionalArguments(resource)
        def fmtSpecifiers = resource.value.findAll(fmtSpecifierPattern)

        args += fmtSpecifiers
        return args
    }
}