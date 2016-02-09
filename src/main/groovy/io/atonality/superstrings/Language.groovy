package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
enum Language {
    English("en"),
    Spanish("es"),
    Arabic("ar");

    String googleCode
    Language(String googleCode) {
        this.googleCode = googleCode
    }
}