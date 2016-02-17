package io.atonality.superstrings

import groovy.transform.CompileStatic

// https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
// TODO: handle regions properly
@CompileStatic
enum Language {
    Arabic('ar', 'ar'),
    Chinese('zh', 'zh-CN'),
    Czech('cs', 'cs-CZ'),
    Dutch('nl', 'nl-NL'),
    English('en', 'en-US'),
    French('fr', 'fr-FR'),
    German('de', 'de-DE'),
    Hindi('hi', 'hi-IN'),
    Indonesian('id', 'id'),
    Italian('it', 'it-IT'),
    Japanese('ja', 'ja-JP'),
    Korean('ko', 'ko-KR'),
    Persian('fa', 'fa'),
    Polish('pl', 'pl-PL'),
    Portuguese('pt', 'pt-PT'),
    Romanian('ro', 'ro'),
    Russian('ru', 'ru-RU'),
    Serbian('sr', 'sr'),
    Slovak('sk', 'k'),
    Spanish('es', 'es-ES'),
    Swedish('sv', 'sv-SE'),
    Thai('th', 'th'),
    Turkish('tr', 'tr-TR'),
    Vietnamese('vi', 'vi');

    String isoCode
    String googlePlayCode
    Language(String isoCode, String googlePlayCode) {
        this.isoCode = isoCode
        this.googlePlayCode = googlePlayCode
    }
}