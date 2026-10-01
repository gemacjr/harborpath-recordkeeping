package com.harborpath.recordkeeping.auth;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harborpath.recordkeeping.config.HarborpathProperties;

@Configuration
@EnableConfigurationProperties({ JwtProperties.class, HarborpathProperties.class })
public class SecurityConfig {
	private final JwtAuthFilter jwtAuthFilter;
	private final HarborpathProperties harborpathProperties;
	private final ObjectMapper objectMapper;

	public SecurityConfig(JwtAuthFilter jwtAuthFilter, HarborpathProperties harborpathProperties,
			ObjectMapper objectMapper) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.harborpathProperties = harborpathProperties;
		this.objectMapper = objectMapper;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(unauthorizedEntryPoint())
						.accessDeniedHandler(accessDeniedHandler()))
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.stream(harborpathProperties.getCors().getOrigins().split(","))
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	private AuthenticationEntryPoint unauthorizedEntryPoint() {
		return (request, response, authException) -> {
			response.setStatus(401);
			response.setContentType("application/json");
			objectMapper.writeValue(response.getOutputStream(),
					java.util.Map.of("error", "That sign-in didn’t match our records."));
		};
	}

	private AccessDeniedHandler accessDeniedHandler() {
		return (request, response, accessDeniedException) -> {
			response.setStatus(403);
			response.setContentType("application/json");
			objectMapper.writeValue(response.getOutputStream(),
					java.util.Map.of("error", "This chair isn’t yours."));
		};
	}
}
