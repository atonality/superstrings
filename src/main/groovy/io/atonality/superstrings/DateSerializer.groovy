package io.atonality.superstrings

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import groovy.transform.CompileStatic

import java.lang.reflect.Type

@CompileStatic
class DateSerializer implements JsonSerializer<Date> {
    @Override
    JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.time as Number)
    }
}