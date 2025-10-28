package com.manhduc205.ezgear.shipping.client;

import com.manhduc205.ezgear.shipping.config.GhnProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnRestClient {

    private final GhnProperties props;

    private RestClient buildClient() {
        return RestClient.builder()
                .baseUrl(props.getActiveBaseUrl())
                .defaultHeader("Token", props.getActiveToken())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public <T> T get(String path, Class<T> responseType) {
        String url = props.getActiveBaseUrl() + path;
        log.info(">>> Calling GHN API: {}", url);
        log.info(">>> Using Token: {}", props.getActiveToken());
        return buildClient()
                .get()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

    public <T> T get(String path, String param, Object value, Class<T> responseType) {
        return buildClient()
                .get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam(param, value).build())
                .retrieve()
                .body(responseType);
    }
}
