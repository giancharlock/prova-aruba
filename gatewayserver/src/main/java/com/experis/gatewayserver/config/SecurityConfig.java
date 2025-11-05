package com.experis.gatewayserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.WebFilter;

import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String SDI_ROLE = "ROLE_SDI";
    private static final String ARUBA_ROLE = "ROLE_ARUBA";
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            @Value("${api.security.sdi-token}") String sdiToken,
                                                            @Value("${api.security.aruba-token}") String arubaToken) {
        http
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/receiver/api/notificaSdI").hasRole("SDI")
                .pathMatchers("/receiver/api/salvaFatturaInterna", "/receiver/api/salvaFatturaEsterna").hasRole("ARUBA")
                .pathMatchers("/dbmanager/**").hasRole("ARUBA")
                .anyExchange().authenticated()
            )
            .addFilterAt(apiKeyAuthenticationFilter(sdiToken, arubaToken), SecurityWebFiltersOrder.AUTHENTICATION)
            .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

    private static WebFilter apiKeyAuthenticationFilter(String sdiToken, String arubaToken) {
        return (exchange, chain) -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

            if (apiKey == null) {
                return chain.filter(exchange);
            }

            if (apiKey.trim().equalsIgnoreCase(sdiToken)) {
                var authorities = Collections.singletonList(new SimpleGrantedAuthority(SDI_ROLE));
                var authentication = new UsernamePasswordAuthenticationToken(null, null, authorities);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            } else if (apiKey.trim().equalsIgnoreCase(arubaToken)) {
                var authorities = Collections.singletonList(new SimpleGrantedAuthority(ARUBA_ROLE));
                var authentication = new UsernamePasswordAuthenticationToken(null, null, authorities);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            }

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }
}
