package com.manhduc205.ezgear.shipping.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ghn")
public class GhnProperties {
    private String env;
    private BaseUrl baseUrl;
    private Token token;
    private ShopId shopId;

    @Getter @Setter
    public static class BaseUrl {
        private String staging;
        private String production;
    }
    @Getter @Setter
    public static class Token {
        private String staging;
        private String production;
    }
    @Getter @Setter
    public static class ShopId {
        private String staging;
        private String production;
    }

    public String getActiveBaseUrl() {
        return "production".equalsIgnoreCase(env) ? baseUrl.production : baseUrl.staging;
    }
    public String getActiveToken() {
        return "production".equalsIgnoreCase(env) ? token.production : token.staging;
    }
    public String getActiveShopId() {
        return "production".equalsIgnoreCase(env) ? shopId.production : shopId.staging;
    }
}
