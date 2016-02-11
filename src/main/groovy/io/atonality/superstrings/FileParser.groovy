package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
interface FileParser {

    List<StringResource> parse(File file) throws RuntimeException;
    SuperstringsMetadata parseMetadata(File file) throws RuntimeException;
}