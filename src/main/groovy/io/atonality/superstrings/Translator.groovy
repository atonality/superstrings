package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
interface Translator {

    double getEstimatedCost(List<Translation> translations);
    TranslationResult translate(Translation translation) throws IOException;
}