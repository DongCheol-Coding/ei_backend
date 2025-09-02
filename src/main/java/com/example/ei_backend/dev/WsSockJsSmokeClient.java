package com.example.ei_backend.dev;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class WsSockJsSmokeClient {

    // 실행 예: java ... WsSockJsSmokeClient ws://localhost:8080 <JWT> 1
    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "ws://localhost:8080";
        String jwt  = args.length > 1 ? args[1] : "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtZW1iZXIxQHRlc3QuY29tIiwicm9sZXMiOlsiUk9MRV9NRU1CRVIiXSwiaWF0IjoxNzU0OTg5NTMxLCJleHAiOjE3NTQ5OTEzMzF9.aQXrZPLvyJsBaMs_1YEzXuzi9JecPc5zj5ePPApOZuk";
        long roomId = args.length > 2 ? Long.parseLong(args[2]) : 1L;

        // ✅ SockJS 대신 네이티브 WebSocket 클라이언트
        var stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        // ✅ SockJS의 raw websocket 엔드포인트로 직접 접속 + 쿼리에도 token 싣기
        String url = base + "/ws-chat/websocket?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        log.info("[CONNECT] " + url);

        // ✅ 핸드셰이크 헤더에 Authorization도 함께 싣기(이중 안전)
        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();
        wsHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

        // (옵션) STOMP CONNECT 헤더도 같이 싣기 — 서버가 안 읽더라도 무해
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

        StompSession session = stomp
                .connect(url, wsHeaders, connectHeaders, new StompSessionHandlerAdapter(){})
                .get(5, TimeUnit.SECONDS);

        CountDownLatch latch = new CountDownLatch(1);

        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return String.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                log.info("[RECV] " + payload);
                latch.countDown();
            }
        });

        var body = Map.of("chatRoomId", roomId, "message", "hi @" + LocalDateTime.now());
        log.info("[SEND] " + body);
        session.send("/app/chat.send", body);

        if (!latch.await(5, TimeUnit.SECONDS)) {
            log.info("수신 없음 (상대 세션/권한/roomId 확인)");
        }

        session.disconnect();
        log.info("[DONE]");
    }
}
