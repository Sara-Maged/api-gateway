package com.example.api_gateway.controller;

import com.example.api_gateway.config.JwtUtil;
import com.example.api_gateway.dto.UserCredentials;
import com.example.api_gateway.dto.UserDetails;
import com.example.api_gateway.dto.UserLoginTrackerDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class RegistrationAndLoginController {

    private final WebClient webClient;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public RegistrationAndLoginController(WebClient.Builder webClientBuilder, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.webClient = webClientBuilder.build();
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public Mono<UserDetails> register(@RequestBody UserDetails userDetails) {
        String hashedPassword = passwordEncoder.encode(userDetails.getPassword());
        userDetails.setPassword(hashedPassword);

        return webClient.post()
                .uri("lb://USER-ATTENDANCE-SERVICE/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(userDetails))
                .retrieve()
                .bodyToMono(UserDetails.class);
    }

    @PostMapping("/login")
    public Mono<String> login(@RequestBody UserCredentials userCredentials) {
        return webClient.get()
                .uri("lb://USER-ATTENDANCE-SERVICE/users/username/" + userCredentials.getUsername())
                .retrieve()
                .bodyToMono(UserDetails.class)
                .flatMap(userDetails -> {
                    if (userDetails != null &&
                            passwordEncoder
                                    .matches(userCredentials.getPassword(), userDetails.getPassword())) {
                        String token = jwtUtil.generateToken(userDetails);

                        UserLoginTrackerDto userLoginTrackerDto = new UserLoginTrackerDto();
                        userLoginTrackerDto.setUser(userDetails);

                        webClient.post()
                                .uri("lb://USER-ATTENDANCE-SERVICE/login-tracker")
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(userLoginTrackerDto))
                                .retrieve()
                                .bodyToMono(UserLoginTrackerDto.class);

                        return Mono.just(token);
                    } else {
                        return Mono.just("Authentication failed");
                    }
                });
    }
}
