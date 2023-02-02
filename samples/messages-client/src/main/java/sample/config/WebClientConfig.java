/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.config;

import io.netty.handler.logging.LogLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.util.Arrays;

/**
 * @author Joe Grandja
 * @since 0.0.1
 */
@Configuration
public class WebClientConfig implements WebFluxConfigurer {


	@Bean
	WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
				new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);


		HttpClient httpClient = HttpClient
				.create()
				.wiretap("reactor.netty.http.client.HttpClient",
						LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.apply(oauth2Client.oauth2Configuration())
				.build();
	}

	@Bean
	OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository authorizedClientRepository) {

		RestTemplate accessTokenRestTemplate = new RestTemplate(Arrays.asList(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
		accessTokenRestTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
		accessTokenRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

		OAuth2AuthorizedClientProvider authorizedClientProvider =
				OAuth2AuthorizedClientProviderBuilder.builder()
						.authorizationCode()
						.refreshToken(builder -> {

							DefaultRefreshTokenTokenResponseClient client = new DefaultRefreshTokenTokenResponseClient();
							client.setRestOperations(accessTokenRestTemplate);

							builder.accessTokenResponseClient(client);
						})
						.clientCredentials(builder -> {

							DefaultClientCredentialsTokenResponseClient client = new DefaultClientCredentialsTokenResponseClient();
							client.setRestOperations(accessTokenRestTemplate);

							builder.accessTokenResponseClient(client);
						})
						.build();

		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);


		return authorizedClientManager;
	}
}
