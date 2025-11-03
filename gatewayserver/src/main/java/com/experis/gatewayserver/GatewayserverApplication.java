package com.experis.gatewayserver;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverApplication.class, args);
	}

	@Bean
	public RouteLocator invoiceAppRouteConfig(RouteLocatorBuilder routeLocatorBuilder) {
		return routeLocatorBuilder.routes()
						.route(p -> p
								.path("/experis/receiver/**")
								.filters( f -> f.rewritePath("/experis/receiver/(?<segment>.*)","/${segment}")
										.circuitBreaker(config -> config.setName("receiverCircuitBreaker")
												.setFallbackUri("forward:/contactSupport")))
								.uri("lb://RECEIVER"))
					.route(p -> p
							.path("/experis/dbmanager/**")
							.filters( f -> f.rewritePath("/experis/dbmanager/(?<segment>.*)","/${segment}")
									.requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter())
											.setKeyResolver(userKeyResolver())))
							.uri("lb://DBMANAGER"))
                .route(p -> p
                        .path("/experis/sender/**")
                        .filters( f -> f.rewritePath("/experis/sender/(?<segment>.*)","/${segment}")
                                .circuitBreaker(config -> config.setName("sendCircuitBreaker")
                                        .setFallbackUri("forward:/contactSupport")))
                        .uri("lb://SENDER"))
                .build();
	}

	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
		return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
				.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
				.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10))
						.build()).build());
	}

	@Bean
	public RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(100, 200, 1);
	}

	@Bean
	KeyResolver userKeyResolver() {
		return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("user"))
				.defaultIfEmpty("anonymous");
	}

}
