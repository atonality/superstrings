package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
enum IoFormat {
    Unknown,
    Android,
    GooglePlay;

    static IoFormat tryParse(String value) {
        try {
            return valueOf(value)
        } catch(IllegalArgumentException ex) {
            return Unknown
        }
    }
}