package io.atonality.superstrings

import groovy.transform.CompileStatic

// TODO: allow specifying which resources are translatable. for now, the app title defaults to false
@CompileStatic
class GooglePlayFileParser extends FileParser {

    static final String APP_TITLE_ID = 'appTitle'
    static final String SHORT_DESCRIPTION_ID = 'shortDescription'
    static final String FULL_DESCRIPTION_ID = 'fullDescription'

    @Override
    SuperstringsMetadata parseMetadata(File file) throws RuntimeException {
        def metadata = new SuperstringsMetadata()
        def config = new ConfigSlurper().parse(file.toURL())

        def mappingArg = config.get('mapping', null) as String
        if (mappingArg) {
            metadata.mapping = parseMapping(mappingArg)
        }
        def properNamesArg = config.get('properNames', null) as Set<String>
        if (properNamesArg) {
            metadata.properNames = []
            metadata.properNames.addAll(properNamesArg)
        }
        return metadata
    }

    @Override
    List<StringResource> parse(File file) throws RuntimeException {
        def config = new ConfigSlurper().parse(file.toURL())
        def resources = [APP_TITLE_ID, SHORT_DESCRIPTION_ID, FULL_DESCRIPTION_ID].collect { String id ->
            def value = config.get(id, null) as String
            if (!value) return null

            def translatable = true
            if (id == APP_TITLE_ID) {
                translatable = config.get('translateTitle', false) as boolean
            }
            return new StringResource(id: id, value: value, translatable: translatable)
        }.findAll { it != null }
        return resources
    }
}