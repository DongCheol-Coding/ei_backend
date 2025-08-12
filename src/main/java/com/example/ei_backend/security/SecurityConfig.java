package com.example.ei_backend.security;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.exception.JsonAccessDeniedHandler;
import com.example.ei_backend.exception.JsonAuthenticationEntryPoint;
import com.example.ei_backend.oauth2.CustomOAuth2FailureHandler;
import com.example.ei_backend.oauth2.CustomOAuth2UserService;
import com.example.ei_backend.oauth2.OAuth2SuccessHandler;
import com.example.ei_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.RequestEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;
    private final UserRepository userRepository;

    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    // ✅ 필터는 스프링이 생성한 빈(=@Component)을 주입받아 사용 (new 금지)
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ⛔ JwtAuthenticationFilter @Bean 등록 불필요 (중복 방지)
    // @Bean
    // public JwtAuthenticationFilter jwtAuthenticationFilter(UserRepository userRepository) {
    //     return new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    // }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        var client = new DefaultAuthorizationCodeTokenResponseClient();

        client.setRequestEntityConverter(request -> {
            OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter =
                    new OAuth2AuthorizationCodeGrantRequestEntityConverter();
            RequestEntity<?> entity = defaultConverter.convert(request);

            @SuppressWarnings("unchecked")
            MultiValueMap<String, String> body = (MultiValueMap<String, String>) entity.getBody();

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
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // ✅ WebSocket/SockJS 전용: 어떤 Origin이 와도 허용 (테스트용)
        CorsConfiguration ws = new CorsConfiguration();
        ws.setAllowedOriginPatterns(java.util.List.of("*")); // allowCredentials=true면 patterns 사용
        ws.setAllowedMethods(java.util.List.of("GET","POST","OPTIONS"));
        ws.setAllowedHeaders(java.util.List.of("*"));
        ws.setAllowCredentials(true);
        ws.setMaxAge(3600L);
        source.registerCorsConfiguration("/ws-chat", ws);
        source.registerCorsConfiguration("/ws-chat/**", ws);

        // ✅ 나머지 API: 기존처럼 제한된 Origin만 허용
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOrigins(java.util.List.of(
                "http://localhost:8082",
                "http://127.0.0.1:8082",
                "http://localhost:5173",
                "http://localhost:63342",      // ★ 추가
                "http://127.0.0.1:63342",      // ★ 추가
                "https://www.dongcheolcoding.life",
                "https://dongcheolcoding.life",
                "http://dongcheolcoding.life",      // ★ 추가
                "http://api.dongcheolcoding.life"
        ));
        api.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        api.setAllowedHeaders(java.util.List.of("*"));
        api.setExposedHeaders(java.util.List.of("Authorization","Location"));
        api.setAllowCredentials(true);
        api.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", api);

        return source;
    }
}
