package io.atonality.superstrings

import groovy.transform.CompileStatic

// https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
@CompileStatic
enum Language {
    Arabic('ar'),
    Chinese('zh'),
    Dutch('nl'),
    English('en'),
    French('fr'),
    German('de'),
    Hindi('hi'),
    Italian('it'),
    Japanese('ja'),
    Korean('ko'),
    Persian('fa'),
    Polish('pl'),
    Portuguese('pt'),
    Russian('ru'),
    Slovak('sk'),
    Spanish('es'),
    Thai('th'),
    Turkish('tr'),
    Vietnamese('vi');

    String isoCode
    Language(String googleCode) {
        this.isoCode = googleCode
    }
}