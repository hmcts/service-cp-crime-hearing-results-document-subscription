package uk.gov.hmcts.cp.subscription.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class JsonMapper {

    private ObjectMapper objectMapper = new ObjectMapper();

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
}
