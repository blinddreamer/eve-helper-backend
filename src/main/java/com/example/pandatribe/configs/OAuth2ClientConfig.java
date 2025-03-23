package com.example.pandatribe.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {

    @Value("${EVE_CLIENT_ID}")
    private String clientId;

    @Value("${EVE_CLIENT_SECRET}")
    private String clientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(this.eveOnlineClientRegistration());
    }

    private ClientRegistration eveOnlineClientRegistration() {
        return ClientRegistration.withRegistrationId("eve")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationUri("https://login.eveonline.com/v2/oauth/authorize")
                .tokenUri("https://login.eveonline.com/v2/oauth/token")
                .userInfoUri("https://esi.evetech.net/verify")
                .userNameAttributeName("CharacterName")
                .scope("publicData", "esi-wallet.read_character_wallet.v1")
                .redirectUri("{baseUrl}/api/v1/callback")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientName("EVE Online")
                .build();
    }
}
