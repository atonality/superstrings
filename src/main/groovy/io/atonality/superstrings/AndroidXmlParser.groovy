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
            return new StringResource(id: id, value: value, translatable: translatable)
        } as List<StringResource>
        resources.removeAll { it == null }
        return resources
    }
}