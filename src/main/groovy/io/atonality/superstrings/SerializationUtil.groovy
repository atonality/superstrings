package io.atonality.superstrings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import groovy.transform.CompileStatic

import java.lang.reflect.Type
import java.text.SimpleDateFormat

@CompileStatic
class SerializationUtil {

    static SimpleDateFormat dateFormat
    static {
        dateFormat = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss')
        dateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))
    }

    static Gson newGsonInstance() {
        new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateSerializer())
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create()
    }

    @CompileStatic
    static class DateSerializer implements JsonSerializer<Date> {
        @Override
        JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            new JsonPrimitive(dateFormat.format(src).toString())
        }
    }

    @CompileStatic
    static class DateDeserializer implements JsonDeserializer<Date> {
        @Override
        Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            dateFormat.parse(json.asJsonPrimitive.asString)
        }
    }
}