package io.atonality.superstrings

import groovy.transform.CompileStatic

@CompileStatic
class SuperstringsNamespace {

    static final String Url = 'http://superstrings.atonality.io/'
    static final String ValueAttr = "{${Url}}value"
    static final String TranslatableAttr = "{${Url}}translatable"
    static final String CDataAttr = "{${Url}}cdata"
    static final String ProperNamesAttr = "{${Url}}properNames"
}