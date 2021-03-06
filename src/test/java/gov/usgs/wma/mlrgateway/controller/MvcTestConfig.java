package gov.usgs.wma.mlrgateway.controller;

import javax.validation.Validator;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ActiveProfiles("test")
public class MvcTestConfig {
    @Bean
    @Primary
    public Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @MockBean
    public JwtDecoder jwtDecoder;

    @Bean
    @Primary
    public OAuth2AuthorizedClientRepository authorizedClients() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    @Primary
    public ClientRegistrationRepository clients() {
        return new InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId("test")
                .clientId("test")
                .clientSecret("test")
                .clientName("test")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizationUri("localhost")
                .tokenUri("localhost")
                .jwkSetUri("localhost")
                .build()
        );
    }
}