package io.atonality.superstrings

import groovy.transform.CompileStatic
import groovy.util.slurpersupport.Node

@CompileStatic
class AndroidXmlParser implements FileParser {

    @Override
    List<StringResource> parse(File file) {
        def xml = new XmlSlurper().parse(file)
        if (!xml) {
            throw new RuntimeException("Failed to parse .xml file: ${file.absolutePath}")
        }
        def nodes = xml.childNodes().findAll { Node node ->
            node.name() == 'string'
        } as List<Node>

        def sharedProperNames = [] as Set<String>
        def properNamesAttr = xml.getProperty("@superstrings:properNames") as String
        if (properNamesAttr != null) {
            sharedProperNames += parseProperNames(properNamesAttr)
        }
        def resources = nodes.collect { Node node ->
            def id = node.attributes()['name'] as String
            def value = node.text()
            if (id?.isEmpty() || value?.isEmpty()) {
                return null
            }
            def translatableAttr = node.attributes()['translatable'] as String
            boolean translatable = translatableAttr ? Boolean.valueOf(translatableAttr) : true

            def valueAttr = node.attributes()[SuperstringsNamespace.ValueAttr] as String
            if (valueAttr != null && !valueAttr.isEmpty()) {
                value = valueAttr
            }
            def resource = new StringResource(id: id, value: value, translatable: translatable)

            def cdataAttr = node.attributes()[SuperstringsNamespace.CDataAttr] as String
            boolean cdata = cdataAttr ? Boolean.valueOf(cdataAttr) : false
            resource.metadata['cdata'] = cdata

            def properNames = [] as Set<String>
            properNamesAttr = node.attributes()[SuperstringsNamespace.ProperNamesAttr] as String
            if (properNamesAttr != null) {
                properNames += parseProperNames(properNamesAttr)
            }
            properNames += sharedProperNames
            resource.metadata['properNames'] = properNames

            return resource
        } as List<StringResource>
        resources.removeAll { it == null }
        return resources
    }

    protected Set<String> parseProperNames(String properNamesAttr) {
        properNamesAttr.split('\\|').toList().collect() { it.trim() }
                .findAll { !it.isEmpty() }.toSet()
    }
}