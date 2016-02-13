package io.atonality.superstrings

import groovy.transform.CompileStatic

// https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
@CompileStatic
enum Language {
    Arabic('ar'),
    Chinese('zh'),
    Czech('cs'),
    Dutch('nl'),
    English('en'),
    French('fr'),
    German('de'),
    Hindi('hi'),
    Indonesian('id'),
    Italian('it'),
    Japanese('ja'),
    Korean('ko'),
    Persian('fa'),
    Polish('pl'),
    Portuguese('pt'),
    Romanian('ro'),
    Russian('ru'),
    Serbian('sr'),
    Slovak('sk'),
    Spanish('es'),
    Swedish('sv'),
    Thai('th'),
    Turkish('tr'),
    Vietnamese('vi');

    String isoCode
    Language(String googleCode) {
        this.isoCode = googleCode
    }
}