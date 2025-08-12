package com.example.ei_backend.dev;

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

public class WsSockJsSmokeClient {

    // 실행 예: java ... WsSockJsSmokeClient ws://localhost:8080 <JWT> 1
    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "ws://localhost:8080";
        String jwt  = args.length > 1 ? args[1] : "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtZW1iZXIxQHRlc3QuY29tIiwicm9sZXMiOlsiUk9MRV9NRU1CRVIiXSwiaWF0IjoxNzU0OTg4MTc0LCJleHAiOjE3NTQ5ODk5NzR9.MgRrqvw7dPP6YyioWueRinjU_fOn-L-D2HHLIHKpn-A";
        long roomId = args.length > 2 ? Long.parseLong(args[2]) : 1L;

        // ✅ SockJS 대신 네이티브 WebSocket 클라이언트
        var stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        // ✅ SockJS의 raw websocket 엔드포인트로 직접 접속 + 쿼리에도 token 싣기
        String url = base + "/ws-chat/websocket?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        System.out.println("[CONNECT] " + url);

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
                System.out.println("[RECV] " + payload);
                latch.countDown();
            }
        });

        var body = Map.of("chatRoomId", roomId, "message", "hi @" + LocalDateTime.now());
        System.out.println("[SEND] " + body);
        session.send("/app/chat.send", body);

        if (!latch.await(5, TimeUnit.SECONDS)) {
            System.out.println("⚠ 수신 없음 (상대 세션/권한/roomId 확인)");
        }

        session.disconnect();
        System.out.println("[DONE]");
    }
}
