package io.atonality.superstrings

import java.text.NumberFormat

// TODO: parse superstrings namespace in .xml properly
// TODO: handle proper names inside resources
// TODO: handle format string specifiers inside resources

// TODO: add parameters and options
// -l input language
// -i target language
// -e exclude language
// -f input file format (Android, etc)
// -c cache file location
// -t timeout
// -d dry run
def cli = new CliBuilder(usage: "superstrings <filepath>")
cli.with {
    h longOpt: 'help', "show usage information"
    g longOpt: 'google-api-key', args: 1, argName: 'key', 'use google translate with specified API key'
}
def options = cli.parse(args);
if (options.h) {
    cli.usage()
    return
}
if (options.arguments()?.size() != 1) {
    cli.usage()
    return
}
def googleApiKey = options.g ? options.g as String : null
if (!googleApiKey || googleApiKey.isAllWhitespace()) {
    println "Missing api key. Please supply -g argument\n"
    cli.usage()
    return
}
// ensure file exists
def file = new File(options.arguments().first())
if (!(file.exists() && file.canRead())) {
    println "Unable to access file: ${file.absolutePath}"
    return
}
// parse file
List<StringResource> resources
Set<StringResource> translatedResources = []
try {
    resources = new AndroidXmlParser().parse(file)
} catch (RuntimeException ex) {
    println "Unable to parse file as Android .xml: ${file.absolutePath}"
    ex.printStackTrace()
    return
}
resources.removeAll { !it.translatable }

// parse cache file
def output = new AndroidOutput()

File cacheFile
if (output != null) {
    cacheFile = output.getCacheFile(file)
} else {
    cacheFile = new File(file.parentFile, "${file.name}.superstrings")
}
cacheFile.getParentFile().mkdirs()
if (cacheFile.exists() && cacheFile.canRead()) {
    try {
        def cachedResources = new JsonParser().parse(cacheFile)
        resources.each { StringResource resource ->
            def cachedValue = cachedResources.find { resource.value == it.value }
            resource.translations = cachedValue?.translations ?: []
        }
        translatedResources += cachedResources
        println "Cache file parsed successfully"
    } catch (RuntimeException ex) {
        println("Unable to parse cache file as .json; all values will be re-translated: ${cacheFile.absolutePath}")
        ex.printStackTrace()
    }
} else {
    println("Unable to access cache file; all values will be re-translated: ${cacheFile.absolutePath}")
}
long timeoutMs = 1000L * 60L * 60L * 24L * 90L // 90 days
def sourceLanguage = Language.English
def targetLanguages = Language.values().toList().toSet()
targetLanguages.remove(sourceLanguage)

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

// print items to be translated / ask if user is ready to translate
def translator = new GoogleTranslator(googleApiKey, sourceLanguage)
def sanitizer = new AndroidSanitizer()

def cost = NumberFormat.getCurrencyInstance(Locale.US).format(translator.getEstimatedCost(translations))
int cachedCount = (resources.size() * targetLanguages.size()) - translations.size()

println "Ready to translate: ${translations.size()} items (found ${cachedCount} cached translations)"
println "Estimated cost: ${cost}"

boolean ready = false
while (!ready) {
    println "Start translating? Enter y for yes, n for no, or l to list items"
    println "Enter command: "
    def command = System.in.newReader().readLine().toLowerCase()

    if (command.startsWith('y')) {
        ready = true
    } else if (command.startsWith('n')) {
        println "Quitting"
        return
    } else if (command.startsWith('l')) {
        for (int i = 0; i < translations.size(); i++) {
            def translation = translations.get(i)
            println "${i + 1}. ${sourceLanguage}->${translation.targetLanguage}: ${translation.resource.value}"
        }
    }
}
println()
println "********************************************************************************"
println "* Start Translating                                                            *"
println "********************************************************************************"

// translate items
int successCount = 0
int failedCount = 0

for (int i = 0; i < translations.size(); i++) {
    def item = translations[i]
    println "Translating text ${sourceLanguage}->${item.targetLanguage}: ${item.resource.value}"

    sanitizer.sanitize(item.resource)
    println "\tSanitized text: ${item.resource.sanitizedValue}"
    try {
        def translation = translator.translate(item)
        println "Translation succeeded: ${translation.translatedValue}"

        sanitizer.rebuild(translation)
        println "\tRebuilt value: ${translation.translatedValue}"
        successCount++

        // cache results
        def resourceToUpdate = translatedResources.find { it == item.resource }
        if (!resourceToUpdate) {
            resourceToUpdate = item.resource.clone() as StringResource
            translatedResources << resourceToUpdate
        }
        resourceToUpdate.translations << translation
        try {
            def json = SerializationUtil.newGsonInstance().toJson(translatedResources)
            cacheFile.withWriter { it << json }
            println "Cache file updated successfully"
        } catch (IOException ex) {
            println "Failed to update cache file"
            ex.printStackTrace()
            return
        }
    } catch (IOException ex) {
        failedCount++
        println "Translation failed"
        ex.printStackTrace()
    }
    println()
}

println()
println "********************************************************************************"
println "* Finished Translating                                                         *"
println "********************************************************************************"
println "${successCount}/${translations.size()} succeeded"
println "${failedCount}/${translations.size()} failed"

println()
println "********************************************************************************"
println "* Start Writing Output Files                                                   *"
println "********************************************************************************"

targetLanguages.each { Language targetLanguage ->
    println("Write output file for language: ${targetLanguage}")
    try {
        def result = output.writeTranslations(translatedResources, targetLanguage, file)
        def outputFile = result.get(0) as File
        def outputResources = result.get(1) as List<StringResource>
        println("Successfully wrote ${outputResources.size()} resources to file: ${outputFile.absolutePath}")
    } catch (IOException ex) {
        println('Failed to write output file')
        ex.printStackTrace()
    }
    println()
}
println('\nFinished')