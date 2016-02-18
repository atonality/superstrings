package io.atonality.superstrings

import groovy.transform.CompileStatic
import groovy.util.slurpersupport.Node

@CompileStatic
class AndroidXmlParser extends FileParser {

    @Override
    SuperstringsMetadata parseMetadata(File file) throws RuntimeException {
        def metadata = new SuperstringsMetadata()

        def xml = new XmlSlurper().parse(file)
        if (!xml) {
            throw new RuntimeException("Failed to parse .xml file: ${file.absolutePath}")
        }
        def properNamesAttr = xml.getProperty('@superstrings:properNames') as String
        if (properNamesAttr != null) {
            metadata.properNames += parseProperNames(properNamesAttr)
        }

        def mappingAttr = xml.getProperty('@superstrings:mapping') as String
        if (mappingAttr != null) {
            metadata.mapping += parseMapping(mappingAttr)
        }
        return metadata
    }

    @Override
    List<StringResource> parse(File file) {
        def xml = new XmlSlurper().parse(file)
        if (!xml) {
            throw new RuntimeException("Failed to parse .xml file: ${file.absolutePath}")
        }
        def nodes = xml.childNodes().findAll { Node node ->
            node.name() == 'string'
        } as List<Node>

        def resources = nodes.collect { Node node ->
            def id = node.attributes()['name'] as String
            def value = node.text()
            if (id?.isEmpty() || value?.isEmpty()) {
                return null
            }
            boolean translatable = true
            ['translatable', SuperstringsNamespace.TranslatableAttr].each {
                // either 'translatable'=> false or 'superstrings:translatable'=> false results
                // disabled translation. translation is enabled by default.
                def translatableAttr = node.attributes()[it] as String
                translatable &= (translatableAttr ? Boolean.valueOf(translatableAttr) : true)
            }
            def valueAttr = node.attributes()[SuperstringsNamespace.ValueAttr] as String
            if (valueAttr != null && !valueAttr.isEmpty()) {
                value = valueAttr
            }
            def resource = new StringResource(id: id, value: value, translatable: translatable)

            def cdataAttr = node.attributes()[SuperstringsNamespace.CDataAttr] as String
            boolean cdata = cdataAttr ? Boolean.valueOf(cdataAttr) : false
            resource.metadata['cdata'] = cdata

            def properNames = [] as Set<String>
            def properNamesAttr = node.attributes()[SuperstringsNamespace.ProperNamesAttr] as String
            if (properNamesAttr != null) {
                properNames += parseProperNames(properNamesAttr)
            }
            resource.metadata['properNames'] = properNames

            return resource
        } as List<StringResource>
        resources.removeAll { it == null }
        return resources
    }
}