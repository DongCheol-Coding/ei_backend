package com.example.ei_backend.security;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.oauth2.CustomOAuth2FailureHandler;
import com.example.ei_backend.oauth2.CustomOAuth2UserService;
import com.example.ei_backend.oauth2.OAuth2SuccessHandler;
import com.example.ei_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.RequestEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.MultiValueMap;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserRepository userRepository) {
        return new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",                      // 루트
                                "/swagger-ui/**",         // Swagger UI 리소스
                                "/v3/api-docs/**",        // Swagger JSON
                                "/swagger-resources/**",  // Swagger 리소스
                                "/webjars/**",            // JS/CSS
                                "/swagger-ui.html",       // 옛 주소
                                "/docs",                  // springdoc의 경로
                                "/docs/**",
                                "/api/auth/**",           // 로그인 등
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers("/api/s3/upload").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String accept = request.getHeader("Accept");

                            if (accept != null && accept.contains("text/html")) {
                                // Swagger UI와 같은 HTML 요청은 리디렉션 또는 허용
                                response.sendRedirect("/swagger-ui/index.html"); // 또는 "/docs" 사용 시 "/docs/index.html"
                            } else {
                                // 일반 API 요청은 JSON 에러 응답
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write("{\"error\": \"Unauthorized\"}");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"error\": \"Forbidden\"}");
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(accessTokenResponseClient())
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(customOAuth2FailureHandler)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * ✅ Spring Security 6.5+ 대응용 Kakao Token Client 설정
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        var client = new DefaultAuthorizationCodeTokenResponseClient();

        client.setRequestEntityConverter(request -> {
            OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
            RequestEntity<?> entity = defaultConverter.convert(request);

            MultiValueMap<String, String> body = (MultiValueMap<String, String>) entity.getBody();

            // ✅ request는 OAuth2AuthorizationCodeGrantRequest 타입임 (getClientRegistration() 사용 가능)
            body.add(OAuth2ParameterNames.CLIENT_ID, request.getClientRegistration().getClientId());
            body.add(OAuth2ParameterNames.CLIENT_SECRET, request.getClientRegistration().getClientSecret());

            return new RequestEntity<>(body, entity.getHeaders(), entity.getMethod(), entity.getUrl());
        });

        return client;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
            return new UserDetailsImpl(user);
        };
    }

}
