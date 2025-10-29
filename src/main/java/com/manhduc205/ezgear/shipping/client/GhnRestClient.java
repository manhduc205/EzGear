package com.manhduc205.ezgear.shipping.client;

import com.manhduc205.ezgear.shipping.config.GhnProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnRestClient {

    private final GhnProperties props;

    private RestClient buildClient() {
        return RestClient.builder()
                .baseUrl(props.getActiveBaseUrl())
                .defaultHeader("Token", props.getActiveToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public <T> T get(String path, Class<T> responseType) {
        String url = props.getActiveBaseUrl() + path;
        log.info(">>> Calling GHN API: {}", url);
        log.info(">>> Using Token: {}", props.getActiveToken());
        try {
            return buildClient()
                    .get()
                    .uri(path)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {
            log.error(" GHN GET error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public <T> T get(String path, String param, Object value, Class<T> responseType) {
        try {
            return buildClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(path).queryParam(param, value).build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {
            log.error(" GHN GET error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
    // T : request R: response
    public <T, R> R post(String path, T body, Class<R> responseType) {
        String fullUrl = props.getActiveBaseUrl() + path;
        log.info(">>> [GHN] POST {}", fullUrl);
        log.info(">>> Using Token: {}", props.getActiveToken());
        try {
            return buildClient()
                    .post()
                    .uri(path)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {
            log.error(" GHN POST error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
