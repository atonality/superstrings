package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
abstract class FileParser {

    abstract List<StringResource> parse(File file) throws RuntimeException;
    abstract SuperstringsMetadata parseMetadata(File file) throws RuntimeException;

    protected Set<String> parseProperNames(String properNamesAttr) {
        properNamesAttr.split('\\|').toList().collect() { it.trim() }
                .findAll { !it.isEmpty() }.toSet()
    }

    protected Map<String, String> parseMapping(String mappingAttr) {
        def result = [:]
        mappingAttr.split('\\|').toList().each {
            def attrs = it.split(':')
            if (attrs.length == 2) {
                result[attrs[0]] = attrs[1]
            }
        }
        return result
    }
}