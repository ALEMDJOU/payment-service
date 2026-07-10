package com.yowyob.payment.infrastructure.support;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Sérialisation JSON pour la persistance R2DBC.
 */
public final class JsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private JsonSupport() {
    }

    /**
     * @param json chaîne JSON
     * @return map métadonnées
     */
    public static Map<String, String> readStringMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(json, STRING_MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON metadata invalide", e);
        }
    }

    /**
     * @param map map à sérialiser
     * @return JSON
     */
    public static String writeStringMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Impossible de sérialiser les métadonnées", e);
        }
    }

    /**
     * @param value objet à sérialiser
     * @return JSON
     */
    public static String writeValue(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Impossible de sérialiser le payload webhook", e);
        }
    }

    /**
     * @return mapper partagé
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
