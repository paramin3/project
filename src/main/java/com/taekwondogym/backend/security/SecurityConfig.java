package com.taekwondogym.backend.security;

import java.util.List;

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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("https://project-production-1cc9.up.railway.app")); // Frontend origin
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            return config;
        }))
        .csrf(csrf -> csrf
                .disable())
        .authorizeHttpRequests(auth -> auth
        	    // Public endpoints
        	    .requestMatchers("/", "/api/users/register", "/api/users/login").permitAll()
        	    .requestMatchers("/css/**", "/js/**").permitAll()
        	    .requestMatchers("/login", "/register", "/shop").permitAll()
        	    .requestMatchers("/uploads/images/**", "/uploads/**").permitAll()
        	    
        	    // Admin-only endpoints
        	    .requestMatchers("/prod").hasAnyRole("ADMIN", "STAFF")
        	    
        	    // Trainer endpoints
        	    .requestMatchers(HttpMethod.GET, "/api/trainers/**").permitAll()
        	    .requestMatchers(HttpMethod.POST, "/api/trainers/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.PUT, "/api/trainers/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.DELETE, "/api/trainers/**").hasRole("ADMIN")

        	    // TrainingClass endpoints
        	    .requestMatchers(HttpMethod.GET, "/api/classes/**").permitAll()
        	    .requestMatchers(HttpMethod.POST, "/api/classes/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.PUT, "/api/classes/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.DELETE, "/api/classes/**").hasRole("ADMIN")

        	    // Product endpoints
        	    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
        	    .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

        	    // Cart endpoints (unchanged)
        	    .requestMatchers("/cart", "/api/cart/**").permitAll()
        	    .requestMatchers(HttpMethod.GET, "/api/products/{id}/check-stock").permitAll()
        	    .requestMatchers(HttpMethod.POST, "/api/cart/products/**").permitAll()
        	    .requestMatchers(HttpMethod.PUT, "/api/cart/products/**").permitAll()
        	    .requestMatchers(HttpMethod.DELETE, "/api/cart/products/**").permitAll()

        	    // Achievement endpoints
        	    .requestMatchers(HttpMethod.GET, "/api/achievements", "/api/achievements/", "/api/achievements/{id}").permitAll()
        	    .requestMatchers(HttpMethod.POST, "/api/achievements/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.PUT, "/api/achievements/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.DELETE, "/api/achievements/**").hasRole("ADMIN")

        	    // Member endpoints
        	    .requestMatchers("/api/members/current").authenticated()
        	    .requestMatchers("/api/members/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.GET, "/api/members/{id}").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.PUT, "/api/members/{id}").hasAnyRole("ADMIN", "STAFF")

        	    // Order endpoints
        	    .requestMatchers(HttpMethod.GET, "/api/orders/user/{email}/**").authenticated()
        	    .requestMatchers(HttpMethod.POST, "/api/orders/**").authenticated()
        	    .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("ADMIN", "STAFF")
        	    .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")

        	    // Role endpoints
        	    .requestMatchers("/role").hasRole("ADMIN")
        	    .requestMatchers(HttpMethod.PUT, "/api/users/change-role/{email}").hasRole("ADMIN")
        	    
        	    // Activity logs (add this if missing)
        	    .requestMatchers("/activity-logs").hasRole("ADMIN")
        	    .requestMatchers("/api/activity-logs/**").hasRole("ADMIN")

        	    // All other requests require authentication
        	    .anyRequest().authenticated()
        	)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
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
