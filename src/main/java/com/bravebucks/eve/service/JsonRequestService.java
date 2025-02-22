package com.bravebucks.eve.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;

import com.mashape.unirest.request.body.RequestBodyEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Deprecated
public class JsonRequestService {

    private static final String WRONG_STATUS_CODE = "{} returned status code {}.";
    private static final String UNIREST_EXCEPTION = "Failed to get data from url={}";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Map<String, String> defaultHeaders;

    @Deprecated
    public JsonRequestService() {
        defaultHeaders = new HashMap<>();
        defaultHeaders.put("User-Agent", "https://github.com/bravecollective/brave-bucks");
        defaultHeaders.put("Accept-Encoding", "gzip");

        // Only one time
        Unirest.setObjectMapper(new ObjectMapper() {
            private final com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Deprecated
    public Optional<Long> searchSolarSystem(final String systemName) {
        String url = "https://esi.evetech.net/latest/universe/ids/?datasource=tranquility&language=en";
        RequestBodyEntity postRequest = post(url, "[\""+systemName+"\"]");
        Optional<JsonNode> optional = executeRequest(postRequest);
        if (optional.isPresent()) {
            JSONObject object = optional.get().getObject();
            if (object.has("systems")) {
                JSONArray systems = object.getJSONArray("systems");
                if (systems.opt(0) != null) {
                    Long systemId = systems.getJSONObject(0).getLong("id");
                    return Optional.of(systemId);
                }
            }
        }
        return Optional.empty();
    }

    Optional<JsonNode> executeRequest(final BaseRequest request) {
        try {
            HttpResponse<JsonNode> response = request.asJson();
            if (response.getStatus() != 200) {
                log.warn(WRONG_STATUS_CODE, request.getHttpRequest().getUrl(), response.getStatus());
                return Optional.empty();
            }
            return Optional.of(response.getBody());
        } catch (UnirestException e) {
            log.error(UNIREST_EXCEPTION, request.getHttpRequest().getUrl(), e);
            return Optional.empty();
        }
    }

    RequestBodyEntity post(String url, String body) {
        return Unirest.post(url).headers(defaultHeaders).body(body);
    }
}
