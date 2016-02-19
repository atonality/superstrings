package io.atonality.superstrings

import java.text.NumberFormat

// TODO: store unsanitized value in cache file, and always re-sanitize before writing.
//       this allows refactoring the sanitizer without requiring retranslation of resources
// TODO: package as jar / runnable application
// TODO: documentation
// TODO: parse superstrings namespace in .xml properly
// TODO: display progress while translating / submitting output
// TODO: add invalidation commands: by id, by language or all

// TODO: add parameters and options
// -i input language
// -e exclude language
// -f input file format (Android, etc)
// -c cache file location
def cli = new CliBuilder(usage: "superstrings <filepath>")
cli.with {
    // usage
    h longOpt: 'help', 'show usage information'

    // required arguments
    f longOpt: 'format', args: 1, argName: 'Android|GooglePlay', 'input / output file format (required)'
    g longOpt: 'google-api-key', args: 1, argName: 'key', 'use google translate with specified API key (required)'

    // format-specific required arguments
    p longOpt: 'package-name', args: 1, argName: 'package', 'android app package name (required if --format="googlePlay")'
    i longOpt: 'service-account-id', args: 1, argName: 'email', 'google play service account id (required if --format="googlePlay")'
    k longOpt: 'private-key', args: 1, argName: 'filepath', 'google play service account private key file (required if --format="googlePlay")'

    // optional arguments
    s longOpt: 'strings', args: 1, argName: 'ids', 'list of string resource IDs to translate, separated by "|"'
    l longOpt: 'languages', args: 1, argName: 'names', 'list of language names, separated by "|"'
    r longOpt: 'retranslate', 'force retranslation of all resources, regardless of existing translations in cache file'
    n longOpt: 'disable-translation', 'skip translation; update outputs only'
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
def formatArg = options.f ? options.f as String : null
if (!formatArg) {
    println "Missing file format. Please supply -f argument\n"
    cli.usage()
    return
}
def format = IoFormat.tryParse(formatArg)
if (format == IoFormat.Unknown) {
    println "Invalid file format ${formatArg}\n"
    cli.usage()
    return
}
def googleApiKey = options.g ? options.g as String : null
if (!googleApiKey || googleApiKey.isAllWhitespace()) {
    println "Missing api key. Please supply -g argument\n"
    cli.usage()
    return
}

// parse format-specific command line arguments
def formatArgs = [:]
if (format == IoFormat.GooglePlay) {
    def packageName = options.p ? options.p as String : null
    if (!packageName) {
        println "Missing package name. Please supply -p argument\n"
        cli.usage()
        return
    }
    def serviceAccountId = options.i ? options.i as String : null
    if (!serviceAccountId) {
        println "Missing service account id. Please supply -i argument\n"
        cli.usage()
        return
    }
    def privateKeyFile = options.k ? options.k as String : null
    if (!privateKeyFile) {
        println "Missing private key filepath. Please supply -k argument\n"
        cli.usage()
        return
    }
    formatArgs['packageName'] = packageName
    formatArgs['serviceAccountId'] = serviceAccountId
    formatArgs['privateKeyFile'] = new File(privateKeyFile)
}

// parse other command line arguments
List<String> stringsArgList = null
def stringsArg = options.s ? options.s as String : null
if (stringsArg) {
    stringsArgList = stringsArg.split('\\|').collect { it.trim() }
    println "Found -s option. Only translating resources with ids: ${stringsArgList.toListString()}"
}
Set<Language> targetLanguages = null
def targetLanguagesArg = options.l ? options.l as String : null
if (targetLanguagesArg) {
    targetLanguages = targetLanguagesArg.split('\\|').collect { Language.tryParse(it) }.findAll { it != null }.toSet()
    println "Found -l option. Target languages: ${targetLanguages.toListString()}"
}
def retranslate = options.r ? options.r as Boolean : false
if (retranslate) {
    println 'Found -r option. All resources will be retranslated'
}
def skipTranslation = options.n ? options.n as Boolean : false
if (skipTranslation) {
    println 'Found -n option. Skipping all translations.'
}

// ensure file exists
def file = new File(options.arguments().first())
if (!(file.exists() && file.canRead())) {
    println "Unable to access file: ${file.absolutePath}"
    return
}

// create output
Output output = null
switch (format) {
    case IoFormat.Android: output = new AndroidOutput(file); break
    case IoFormat.GooglePlay:
        def packageName = formatArgs['packageName'] as String
        def serviceAccountId = formatArgs['serviceAccountId'] as String
        def privateKeyFile = formatArgs['privateKeyFile'] as File

        output = new GooglePlayOutput(packageName, serviceAccountId, privateKeyFile)
        break
}

// parse resources
FileParser parser = null
switch (format) {
    case IoFormat.Android: parser = new AndroidXmlParser(); break
    case IoFormat.GooglePlay: parser = new GooglePlayFileParser(); break
}
List<StringResource> resources
Set<StringResource> translatedResources = []
try {
    resources = parser.parse(file)
    output.onResourcesParsed(resources)
} catch (RuntimeException ex) {
    println "Unable to parse resources from Android .xml file: ${file.absolutePath}"
    ex.printStackTrace()
    return
}
resources.removeAll { !it.translatable }

// parse metadata
def metadata = new SuperstringsMetadata()
try {
    metadata = parser.parseMetadata(file)
} catch (RuntimeException ex) {
    println "Unable to parse metadata from Android .xml file: ${file.absolutePath}"
    ex.printStackTrace()
    return
}

// parse cache file
File cacheFile = null
if (output != null) {
    cacheFile = output.getCacheFile()
}
if (!cacheFile) {
    cacheFile = new File(file.parentFile, "${file.name}.superstrings")
}
cacheFile.getParentFile().mkdirs()
if (cacheFile.exists() && cacheFile.canRead()) {
    try {
        def cachedResources = new JsonFileParser().parse(cacheFile)

        // remove cached resources which no longer exist in the input file (by value, not id)
        cachedResources.removeAll { StringResource resource ->
            !resources.any { it.value == resource.value }
        }
        // update input resources with existing translations from cache file (again, by value)
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
if (!targetLanguages) {
    targetLanguages = Language.values().toList().toSet();
    targetLanguages.remove(sourceLanguage)
}

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
        if (retranslate) {
            return true
        }
        !resource.translations.any { it.language == targetLanguage }
    }.collect {
        new Translation(resource: resource, targetLanguage: it)
    }
}.flatten() as List<Translation>
if (stringsArgList != null) {
    translations.removeAll { Translation translation ->
        !(translation.resource.id in stringsArgList)
    }
}
if (skipTranslation) {
    translations = []
}

// print items to be translated / ask if user is ready to translate
def translator = new GoogleTranslator(googleApiKey, sourceLanguage)
Sanitizer sanitizer = null
switch (format) {
    case IoFormat.Android: sanitizer = new AndroidSanitizer(metadata); break
    case IoFormat.GooglePlay: sanitizer = new GooglePlaySanitizer(metadata); break
}

def cost = NumberFormat.getCurrencyInstance(Locale.US).format(translator.getEstimatedCost(translations))
int cachedCount = (resources.size() * targetLanguages.size()) - translations.size()

println "Ready to translate: ${translations.size()} items (found ${cachedCount} cached translations)"
println "Estimated cost: ${cost}"

boolean ready = translations.isEmpty()
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

    item.resource.sanitizedValue = sanitizer.sanitize(item.resource)
    println "\tSanitized text: ${item.resource.sanitizedValue}"
    try {
        def translation = translator.translate(item)
        println "Value translated: ${translation.translatedValue}"

        translation.translatedValue = sanitizer.rebuild(translation)
        println "\tRebuilt value: ${translation.translatedValue}"
        successCount++

        // cache results
        def resourceToUpdate = translatedResources.find { it == item.resource }
        if (!resourceToUpdate) {
            resourceToUpdate = item.resource.clone() as StringResource
            translatedResources << resourceToUpdate
        }
        boolean updateTranslation = true
        if (retranslate) {
            def existingTranslation = resourceToUpdate.translations.find {
                it.language == translation.language
            }
            if (existingTranslation) {
                if (existingTranslation.translatedValue == translation.translatedValue) {
                    println 'Retranslated value matches existing translation; Skip updating this resource'
                    updateTranslation = false
                } else {
                    resourceToUpdate.translations.remove(existingTranslation)
                }
            }
        }
        if (updateTranslation) {
            resourceToUpdate.translations << translation
        }
        updateCacheFile(cacheFile, translatedResources)
    } catch (IOException ex) {
        failedCount++
        println "Translation failed"
        ex.printStackTrace()
    }
    println()
}
if (translations.isEmpty()) {
    updateCacheFile(cacheFile, translatedResources)
}
def updateCacheFile(File cacheFile, Set<StringResource> translatedResources) {
    try {
        def sortedOutput = translatedResources.sort() { StringResource left, StringResource right ->
            left.id <=> right.id
        }
        sortedOutput.each {
            it.translations.sort(true) { TranslationResult left, TranslationResult right ->
                left.language <=> right.language
            }
        }
        def json = SerializationUtil.newGsonInstance().toJson(sortedOutput)
        cacheFile.withWriter { it << json }
        println "Cache file updated successfully"
    } catch (IOException ex) {
        println "Failed to update cache file"
        ex.printStackTrace()
    }
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
        def result = output.outputTranslations(translatedResources, targetLanguage)
        println result
    } catch (IOException ex) {
        println('Failed to write output file')
        ex.printStackTrace()
    }
    println()
}
output.finish()
println('\nFinished')