package com.example.myshop.OAuth2;

import com.example.myshop.domain.UserRole;
import com.example.myshop.domain.entity.User;
import com.example.myshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();
            log.info("OAuth2 attributes: {}", attributes); // ✅ 여기 먼저 확인

            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            String email = (String) kakaoAccount.get("email");
            String nickname = extractNickname(kakaoAccount);

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> userRepository.save(User.builder()
                            .email(email)
                            .name(nickname)
                            .password("소셜로그인")
                            .isSocial(true)
                            .roles(Set.of(UserRole.BUYER))
                            .build()));

            return new CustomOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "id",
                    user
            );
        } catch (Exception e) {
            log.error("[OAuth2] 사용자 정보 처리 중 오류 발생", e);
            throw e; // 반드시 다시 던져줘야 Spring이 실패 처리함
        }
    }

    private String extractNickname(Map<String, Object> kakaoAccount) {
        Object profileObj = kakaoAccount.get("profile");
        if (profileObj instanceof Map profileMap && profileMap.get("nickname") != null) {
            return profileMap.get("nickname").toString();
        }
        return "unknown";
    }

}
