package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
class StringResource {

    String id
    String value
    boolean translatable
    List<TranslationResult> translations = []

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new StringResource(id: this.id, value: this.value, translatable: this.translatable)
    }

    @Override
    boolean equals(Object obj) {
        return (obj instanceof StringResource) && (obj as StringResource).value == value
    }
}