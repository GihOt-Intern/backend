package com.server.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

// import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${api.prefix}")
    private String apiPrefix;

    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CustomJwtDecoder customJwtDecoder;


    // private static final String[] PUBLIC_GET_ENDPOINTS = {
    //     "/user/*"
    // };

    // private static final String[] PUBLIC_POST_ENDPOINTS = {
    //     "/auth/**",
    //     // "/user",  // get user my email
    //     "/user/create"
    // };

    // private static final String[] AUTHENTICATED_GET_ENDPOINTS = {
    //     "/user/all"
    // };

    // private String[] mapPrefix(String[] endpoints){
    //     return Arrays.stream(endpoints)
    //         .map(endpoint -> apiPrefix + endpoint)
    //         .toArray(String[]::new);
    // }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        // System.out.println("Mapped public endpoints: " + Arrays.toString(mapPrefix(PUBLIC_POST_ENDPOINTS)));

        httpSecurity
            .csrf(csrf -> csrf.disable())
            // .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"))
            .authorizeHttpRequests(request -> request
                // .requestMatchers(HttpMethod.GET,  this.mapPrefix(AUTHENTICATED_GET_ENDPOINTS))
                // .authenticated()
                // .hasAuthority("SCOPE_ADMIN")
                // .hasRole("ADMIN")
                // .requestMatchers(HttpMethod.POST, this.mapPrefix(PUBLIC_POST_ENDPOINTS)).permitAll()
                // .requestMatchers(
                //     "/ws-chat/**",          // SockJS endpoints
                //     "/ws-chat",             // WebSocket endpoint
                //     "/ws/**",               // nếu bạn dùng WebSocket raw trước đó
                //     "/favicon.ico",
                //     "/", "/index.html",
                //     "/js/**", "/css/**", "/images/**"
                // ).permitAll()
                // .anyRequest().authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(this.customJwtDecoder)
                .jwtAuthenticationConverter(this.jwtAuthenticationConverter())) // convert "SCOPE_" to "ROLE_"
                .authenticationEntryPoint(authenticationEntryPoint)
            );


        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCryptPasswordEncoder for password hashing
        return new BCryptPasswordEncoder();
    }


    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Convert JWT scopes to authorities with "ROLE_" prefix
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
