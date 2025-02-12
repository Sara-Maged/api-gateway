package com.example.api_gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-route", r -> r.path("/user/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter)) // Apply JWT filter
                        .uri("lb://USER-ATTENDANCE-SERVICE")) // Service discovery (application.name)
                .route("job-service-route", r -> r.path("/job/**") // Path at the gateway
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter) // JWT Authentication (re-enable later)
                                .rewritePath("/job/(?<path>.*)", "/job-api/${path}")) // Rewrite the path
                        .uri("lb://JOBMANAGEMENTSVC")) // Use the EXACT application name from properties
                .build();
    }
}
