package uk.gov.hmcts.cp.subscription.http.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class JsonMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public String toJson(final Object object) {
        return objectMapper.writeValueAsString(object);
    }

    @SneakyThrows
    public <T> T fromJson(final String json, final Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    public JsonNode toJsonNode(final String json) {
        return objectMapper.readTree(json);
    }

    @SneakyThrows
    public String getStringAtPath(final String json, final String jsonPointer) {
        return toJsonNode(json).at(jsonPointer).textValue();
    }

    @SneakyThrows
    public int getIntAtPath(final String json, final String jsonPointer) {
        return toJsonNode(json).at(jsonPointer).intValue();
    }
}