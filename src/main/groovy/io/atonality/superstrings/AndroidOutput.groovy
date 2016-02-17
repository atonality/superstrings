package io.atonality.superstrings

import groovy.xml.MarkupBuilder

class AndroidOutput implements Output {

    final File inputFile

    AndroidOutput(File inputFile) {
        this.inputFile = inputFile
    }

    @Override
    File getCacheFile() {
        File file = inputFile
        while (file.getName() != 'src') {
            file = file.getParentFile()
            if (file == null) {
                throw new RuntimeException('Unable to find "src" parent directory of strings.xml file')
            }
        }
        // after below call, file points to the main project directory
        file = file.getParentFile()

        // build .superstrings subdirectory
        file = new File(file, '.superstrings')
        return new File(file, "${inputFile.name}.superstrings")
    }

    @Override
    String outputTranslations(Set<StringResource> translatedResources, Language targetLanguage) throws IOException {
        // filter and sort resources
        translatedResources = translatedResources.findAll { StringResource resource ->
            def translation = resource.translations.find { it.language == targetLanguage }
            return translation != null
        }
        .sort { StringResource left, StringResource right ->
            left.id <=> right.id
        }

        // build output file path
        def inValuesDir = inputFile.getParentFile()
        def outValuesDir = new File(inValuesDir.getParentFile(), "${inValuesDir.getName()}-${targetLanguage.isoCode}")
        def outputFile = new File(outValuesDir, inputFile.getName())
        outputFile.getParentFile().mkdirs()

        // write xml to output file
        def xml = new MarkupBuilder(outputFile.newWriter('utf-8'))
        xml.doubleQuotes = true
        xml.escapeAttributes = false

        xml.mkp.xmlDeclaration(['version':'1.0', 'encoding':'utf-8'])
        xml.resources {
            translatedResources.each { StringResource resource ->
                def translation = resource.translations.find { it.language == targetLanguage }
                xml.string(name: resource.id) {
                    if (resource.metadata['cdata'] == true) {
                        xml.mkp.yieldUnescaped(translation.translatedValue)
                    } else {
                        xml.mkp.yield(translation.translatedValue)
                    }
                }
            }
        }
        return "Successfully wrote ${translatedResources.size()} resources to file: ${outputFile.absolutePath}"
    }

    @Override
    void onResourcesParsed(List<StringResource> resources) {}

    @Override
    void finish() {}
}