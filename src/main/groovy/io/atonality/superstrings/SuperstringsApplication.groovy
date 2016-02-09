package io.atonality.superstrings

import java.text.ParseException

// TODO: add parameters and options
// -l input language
// -i target language
// -e exclude language
// -f input file format (Android, etc)
// -c cache file location
// -t timeout
// -d dry run
def cli = new CliBuilder(usage: "superstrings <filepath>")
def options = cli.parse(args);
if (options.arguments()?.size() != 1) {
    cli.usage();
    return
}
// ensure file exists
def file = new File(options.arguments().first())
if (!file.exists() && file.canRead()) {
    println "Unable to access file: ${file.absolutePath}"
    return
}
// parse file
List<StringResource> resources
try {
    resources = new AndroidXmlParser().parse(file)
} catch (ParseException ex) {
    println "Unable to parse file as Android .xml: ${file.absolutePath}"
    ex.printStackTrace()
    return
}
// parse cache file
def cacheFile = new File(file.parentFile, "${file.name}.superstrings")
if (cacheFile.exists() && cacheFile.canRead()) {
    try {
        def cachedResources = new AndroidXmlParser().parse(cacheFile)
        resources.each { StringResource resource ->
            def cachedValue = cachedResources.find { resource.value == it.value }
            resource.translations = cachedValue?.translations ?: []
        }
    } catch (ParseException ex) {
        println("Unable to parse cache file as .json: ${cacheFile.absolutePath}")
        ex.printStackTrace()
    }
} else {
    println("Cache file does not exist; all values will be re-translated: ${cacheFile.absolutePath}")
}
long timeoutMs = 1000 * 60 * 60 * 24 * 90 // 90 days
def inputLanguage = Language.English
def targetLanguages = Language.values().toList()
targetLanguages.remove(inputLanguage)

// remove stale translations
resources.each { StringResource resource ->
    resource.translations.removeAll { TranslationResult translation ->
        long elapsedMs = System.currentTimeMillis() - translation.dateTranslated?.time ?: 0
        return elapsedMs > timeoutMs
    }
}
// generate translation directives
def translations = resources.collect { StringResource resource ->
    targetLanguages.findAll { Language targetLanguage ->
        !resource.translations.any { it.language == targetLanguage }
    }.collect {
        new Translation(resource: resource, targetLanguage: it)
    }
}.flatten() as List<Translation>
println "${translations.size()} translations needed (run with -d to print the list and quit)"

translations.each {
    println "To ${it.targetLanguage}: ${it.resource.value}"
}
println "Estimated cost: \$0.00"
println "Start translating? (y/n): "