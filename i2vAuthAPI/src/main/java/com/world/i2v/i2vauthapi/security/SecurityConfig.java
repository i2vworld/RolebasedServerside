package com.world.i2v.i2vauthapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:5173,http://localhost:4000,https://i2vuat-test-role-base-system-dhdadebxfhb6bvda.southindia-01.azurewebsites.net}")
    private String allowedOrigins;

    public SecurityConfig(UserDetailsService userDetailsService,
                          JwtAuthenticationEntryPoint unauthorizedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitingFilter rateLimitingFilter) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ENDPOINTS
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/error"
                        ).permitAll()

                        // EMPLOYEE ONLY ENDPOINTS
                        .requestMatchers("/api/leaves/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/attendance/**").authenticated()
                        .requestMatchers("/api/admin/attendance/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/employees/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/module-access/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/departments/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/designations/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/modules/**").hasRole("EMPLOYEE")

                        // Static and public resources
                        .requestMatchers(
                            "/",
                            "/index.html",
                            "/static/**",
                            "/public/**",
                            "/favicon.ico"
                        ).permitAll()

                        // All other API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        // All other requests require authentication by default
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Load allowed origins from environment variable and sanitize
        String allowedOriginsList = this.allowedOrigins == null ? "" : this.allowedOrigins;
        List<String> origins = Arrays.stream(allowedOriginsList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals("*"))
                .toList();

        // If none provided, fall back to a safe localhost origin for development
        if (origins.isEmpty()) {
            origins = List.of("http://localhost:3000");
        }

        // Use explicit allowed origins rather than pattern wildcard to avoid accidental open CORS
        configuration.setAllowedOrigins(origins);

        // Wildcard for methods – OPTIONS is essential
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Use a single "*" string (not inside a list) for all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Expose the Authorization header if your frontend needs to access it
        configuration.setExposedHeaders(List.of("Authorization"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
