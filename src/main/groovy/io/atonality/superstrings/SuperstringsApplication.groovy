package io.atonality.superstrings

import com.google.gson.GsonBuilder

import java.text.NumberFormat

// TODO: add parameters and options
// TODO: add api key as command line parameter
// TODO: handle translatable attribute
// TODO: add and handle translatableValue to Android .xml resources

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
// parse cache file
def cacheFile = new File(file.parentFile, "${file.name}.superstrings")
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
def targetLanguages = Language.values().toList()
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
///
def googleApiKey = ""
///
def translator = new GoogleTranslator(googleApiKey, sourceLanguage)
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

def gson = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(Date.class, new DateSerializer())
    .create()

for (int i = 0; i < 4; i++) {
    def item = translations[i]
    println "Translating text ${sourceLanguage}->${item.targetLanguage}: ${item.resource.value}"
    try {
        def translation = translator.translate(item)
        successCount++
        println "Translation succeeded: ${translation.translatedValue}"

        // cache results
        def resourceToUpdate = translatedResources.find { it == item.resource }
        if (!resourceToUpdate) {
            resourceToUpdate = item.resource.clone() as StringResource
            translatedResources << resourceToUpdate
        }
        resourceToUpdate.translations << translation
        try {
            def json = gson.toJson(translatedResources)
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