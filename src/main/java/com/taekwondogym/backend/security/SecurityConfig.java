package com.taekwondogym.backend.security;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(
                    "https://adorable-freedom-production.up.railway.app", 
                    "https://project-front-6y8f.onrender.com"
                ));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);
                return config;
            }))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/api/users/register", "/api/users/login").permitAll()
                .requestMatchers("/css/**", "/js/**").permitAll()
                .requestMatchers("/login", "/register", "/shop").permitAll()
                .requestMatchers("/uploads/images/**", "/uploads/**").permitAll()
                .requestMatchers("/prod").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/trainers/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/trainers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/trainers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/trainers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/classes/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/classes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/classes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/classes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/products/**")
    .hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/cart", "/api/cart/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/{id}/check-stock").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/cart/products/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/cart/products/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/cart/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/achievements", "/api/achievements/", "/api/achievements/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/achievements/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/achievements/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/achievements/**").hasRole("ADMIN")
                .requestMatchers("/api/members/current").authenticated()
                .requestMatchers("/api/members/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/members/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/members/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders/user/{email}/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
            )
            .formLogin().disable()
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
