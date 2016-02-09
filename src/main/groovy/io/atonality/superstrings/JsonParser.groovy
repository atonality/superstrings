package io.atonality.superstrings

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import groovy.transform.CompileStatic

import java.lang.reflect.Type

@CompileStatic
class JsonParser implements FileParser {

    @Override
    List<StringResource> parse(File file) {
        def token = new TypeToken<List<StringResource>>(){}.getType()
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create().fromJson(file.text, token) as List<StringResource>
    }

    @CompileStatic
    class DateDeserializer implements JsonDeserializer<Date> {
        @Override
        Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Date(json.asJsonPrimitive.asLong)
        }
    }
}