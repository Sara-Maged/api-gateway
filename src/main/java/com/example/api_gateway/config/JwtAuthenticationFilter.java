package com.example.api_gateway.config;

import com.example.api_gateway.dto.UserDetails;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
public class JwtAuthenticationFilter implements GatewayFilter {

    private final JwtUtil jwtUtil;
    private final WebClient webClient;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, WebClient.Builder webClientBuilder) {
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder.build();
    }

    @Override //Gateway Filter that intercepts requests and performs JWT authentication
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!request.getURI().getPath().startsWith("/auth")) { // Exclude authentication endpoints
            final String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    String username = jwtUtil.extractUsername(token);

                    return webClient.get()
                            .uri("lb://USER-ATTENDANCE-SERVICE/users/username/" + username)
                            .retrieve()
                            .bodyToMono(UserDetails.class)
                            .flatMap(userDetails -> {
                                if (userDetails != null) {
                                    if (jwtUtil.validateToken(token, userDetails.getUsername())) {
//                                      This object represents the authenticated user
                                        UsernamePasswordAuthenticationToken authenticationToken =
                                                new UsernamePasswordAuthenticationToken(userDetails,
                                                        null, Collections.emptyList());
//                                      This makes the user's information available to the rest of the application
                                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                                        return chain.filter(exchange);
                                    }
                                }
                                return Mono.just(exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED));
                            })
                            .switchIfEmpty(Mono.just(exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))).then();
                } catch (Exception e) {
                    return Mono.error(new Exception("Invalid token"));
                }
            }
            return Mono.just(exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)).then();
        }
        return chain.filter(exchange);
    }
}
