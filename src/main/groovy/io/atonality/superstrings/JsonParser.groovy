package io.atonality.superstrings

import com.google.gson.reflect.TypeToken
import groovy.transform.CompileStatic

@CompileStatic
class JsonParser implements FileParser {

    @Override
    List<StringResource> parse(File file) {
        def token = new TypeToken<List<StringResource>>(){}.getType()
        SerializationUtil.newGsonInstance().fromJson(file.text, token) as List<StringResource>
    }

    // unused
    @Override
    SuperstringsMetadata parseMetadata(File file) throws RuntimeException {
        return new SuperstringsMetadata()
    }
}