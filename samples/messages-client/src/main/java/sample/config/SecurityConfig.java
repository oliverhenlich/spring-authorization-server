/*
 * Copyright 2020-2022 the original author or authors.
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author Joe Grandja
 * @since 0.0.1
 */
@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring().requestMatchers("/webjars/**");
	}

	// @formatter:off
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize ->
						authorize.anyRequest().authenticated()
				)
				.oauth2Login(oauth2Login -> {
					// Customize the accessTokenResponseClient to use a more informative RestClient instance
					DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
					RestTemplate tokenRestTemplate = new RestTemplate(Arrays.asList(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
					tokenRestTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
					tokenRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
					accessTokenResponseClient.setRestOperations(tokenRestTemplate);

					// Customize the oidcUserService to use a more informative RestClient instance
					DefaultOAuth2UserService oauthUserService = new DefaultOAuth2UserService();
					RestTemplate userServiceRestTemplate = new RestTemplate();
					userServiceRestTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
					userServiceRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
					oauthUserService.setRestOperations(userServiceRestTemplate);

					OidcUserService oidcUserService = new OidcUserService();
					oidcUserService.setOauth2UserService(oauthUserService);

					oauth2Login
							.tokenEndpoint()
								.accessTokenResponseClient(accessTokenResponseClient)
								.and()
							.userInfoEndpoint()
								.oidcUserService(oidcUserService)
								.and()
							.loginPage("/oauth2/authorization/messaging-client-oidc");
				})
			.oauth2Client(withDefaults());
		return http.build();
	}
	// @formatter:on

}
