package com.gorkemuysal.minibank.transfer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AccountServiceClientConfig {

    static final String REGISTRATION_ID = "account-service";

    /**
     * Obtains and caches client_credentials tokens.
     * This manager does not depend on an incoming HTTP request, so it also works
     * outside a web request (e.g. a Kafka consumer in step 4).
     */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                          OAuth2AuthorizedClientService clientService) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clientService);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    RestClient accountServiceRestClient(OAuth2AuthorizedClientManager manager,
                                        @Value("${account-service.base-url}") String baseUrl) {
        // Every remote call needs timeouts; without a read timeout a hung
        // account-service would block our request threads forever.
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        // Attaches "Authorization: Bearer <service token>" to every request,
        // fetching a new token from Keycloak only when the cached one expires.
        var oauth2 = new OAuth2ClientHttpRequestInterceptor(manager);
        // This RestClient only ever talks to account-service, so the registration is fixed.
        oauth2.setClientRegistrationIdResolver(request -> REGISTRATION_ID);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(oauth2)
                .build();
    }
}