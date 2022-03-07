/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.kgraph.config;

import com.mytiki.common.ApiConstants;
import com.mytiki.kgraph.utilities.ApiKeyAuthMgr;
import com.mytiki.kgraph.utilities.ApiKeyFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class ConfigSecurity extends WebSecurityConfigurerAdapter {
    private static final String FEATURE_POLICY = "accelerometer" + " 'none'" + "ambient-light-sensor" + " 'none'" +
            "autoplay" + " 'none'" + "battery" + " 'none'" + "camera" + " 'none'" + "display-capture" + " 'none'" +
            "document-domain" + " 'none'" + "encrypted-media" + " 'none'" + "execution-while-not-rendered" + " 'none'" +
            "execution-while-out-of-viewport" + " 'none'" + "fullscreen" + " 'none'" + "geolocation" + " 'none'" +
            "gyroscope" + " 'none'" + "layout-animations" + " 'none'" + "legacy-image-formats" + " 'none'" +
            "magnetometer" + " 'none'" + "microphone" + " 'none'" + "midi" + " 'none'" + "navigation-override" + " 'none'" +
            "oversized-images" + " 'none'" + "payment" + " 'none'" + "picture-in-picture" + " 'none'" + "publickey-credentials-get" + " 'none'" +
            "sync-xhr" + " 'none'" + "usb" + " 'none'" + "vr wake-lock" + " 'none'" + "xr-spatial-tracking" + " 'none'";

    private static final String CONTENT_SECURITY_POLICY = "default-src" + "' self'";

    @Value("${com.mytiki.kgraph.api_key.ingest}")
    private String ingestApiKey;

    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationEntryPoint authEntryPointImplException;

    public ConfigSecurity(
            @Autowired AccessDeniedHandler accessDeniedHandler,
            @Autowired @Qualifier(value = "authEntryPointImplException")
                    AuthenticationEntryPoint authEntryPointImplException) {
        super(true);
        this.accessDeniedHandler = accessDeniedHandler;
        this.authEntryPointImplException = authEntryPointImplException;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        ApiKeyFilter apiKeyFilter = new ApiKeyFilter();
        apiKeyFilter.setAuthenticationManager(new ApiKeyAuthMgr(ingestApiKey));

        http
                .addFilter(new WebAsyncManagerIntegrationFilter())
                .servletApi().and()
                .exceptionHandling(handler -> handler
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authEntryPointImplException)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext().and()
                .headers(headers -> headers
                        .cacheControl().and()
                        .contentTypeOptions().and()
                        .httpStrictTransportSecurity().and()
                        .frameOptions().and()
                        .xssProtection().and()
                        .referrerPolicy().and()
                        .featurePolicy(FEATURE_POLICY).and()
                        .httpPublicKeyPinning().and()
                        .contentSecurityPolicy(CONTENT_SECURITY_POLICY)
                )
                .anonymous().and()
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )
                .addFilter(new ApiKeyFilter()
                        .setAuthMgr(new ApiKeyAuthMgr(ingestApiKey)))
                .authorizeRequests(authorize -> authorize
                        .antMatchers(HttpMethod.GET, ApiConstants.HEALTH_ROUTE).permitAll()
                        .antMatchers(
                                HttpMethod.POST,
                                ApiConstants.API_LATEST_ROUTE + "big-picture"
                        ).permitAll()
                        .anyRequest()
                        .authenticated()
                );
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET","PUT","POST","DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("*/**", configuration);
        return source;
    }
}
