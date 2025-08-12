package com.example.ei_backend.security;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.exception.JsonAccessDeniedHandler;
import com.example.ei_backend.exception.JsonAuthenticationEntryPoint;
import com.example.ei_backend.oauth2.CustomOAuth2FailureHandler;
import com.example.ei_backend.oauth2.CustomOAuth2UserService;
import com.example.ei_backend.oauth2.OAuth2SuccessHandler;
import com.example.ei_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

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
                //Saved request 캐시 끄기 (불필요한 세션 저장/리다이렉트 방지)
                .requestCache(c -> c.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**",
                                "/webjars/**", "/swagger-ui.html", "/docs", "/docs/**",
                                // 공개 엔드포인트만 지정
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/reissue",
                                "/api/auth/verify/**",
                                "/oauth2/**", "/login/oauth2/**",
                                "/actuator/health"
                        ).permitAll()
                        // 프로필 이미지는 인증 필수
                        .requestMatchers(HttpMethod.PATCH,  "/api/auth/profile/image").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/profile/image").authenticated()
                        .requestMatchers("/api/s3/upload").authenticated()
                        .anyRequest().authenticated()
                )

                // ✅ 401/403을 ApiResponse JSON으로 통일
                .exceptionHandling(ex -> ex
                        // ✅ /api/**는 반드시 JSON 401 (리다이렉트 금지)
                        .defaultAuthenticationEntryPointFor(
                                jsonAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**")
                        )
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )

                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(token -> token.accessTokenResponseClient(accessTokenResponseClient()))
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(customOAuth2FailureHandler)
                )

                .addFilterBefore(jwtAuthenticationFilter(userRepository),
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // ✅ 허용할 프론트엔드 Origin만 명시
        config.setAllowedOrigins(java.util.List.of(
                "http://localhost:5173",
                "https://www.dongcheolcoding.life",
                "https://dongcheolcoding.life",
                "http://dongcheolcoding.life"
        ));

        config.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));          // Authorization 등 모든 요청 헤더 허용
        config.setExposedHeaders(java.util.List.of("Authorization","Location")); // JS에서 읽을 응답 헤더
        config.setAllowCredentials(true);                           // 쿠키/자격증명 사용 시 필수
        config.setMaxAge(3600L);                                    // preflight 캐시
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;

    }

    @Bean
    org.springframework.web.filter.ForwardedHeaderFilter forwardedHeaderFilter() {
        return new org.springframework.web.filter.ForwardedHeaderFilter();
    }

}
