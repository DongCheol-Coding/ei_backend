package com.example.ei_backend.dev;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class WsSockJsSmokeClient {

    // 실행 예:
    //   java ... WsSockJsSmokeClient ws://localhost:8080 <JWT> 1
    //   (IDE에선 Program arguments: ws://localhost:8080 <JWT> 1)
    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "ws://localhost:8080";
        String jwt  = args.length > 1 ? args[1] : "YOUR_JWT";
        long roomId = args.length > 2 ? Long.parseLong(args[2]) : 1L;

        var sockJsClient = new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())));
        var stomp = new WebSocketStompClient(sockJsClient);
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        String url = base + "/ws-chat?token=" + jwt; // ← Handshake 인터셉터가 ?token= 읽음
        System.out.println("[CONNECT] " + url);

        StompSession session = stomp.connectAsync(url, new StompSessionHandlerAdapter(){}).get(5, TimeUnit.SECONDS);

        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);

        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return String.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                String body = String.valueOf(payload);
                System.out.println("[RECV] " + body);
                inbox.offer(body);
                latch.countDown();
            }
        });

        // 전송
        var body = Map.of("chatRoomId", roomId, "message", "hi @" + LocalDateTime.now());
        System.out.println("[SEND] " + body);
        session.send("/app/chat.send", body);

        // 수신 대기 (상대 세션이 있어야 수신됨)
        if (!latch.await(5, TimeUnit.SECONDS)) {
            System.out.println("⚠ 수신 없음 (상대가 접속/구독 중인지 확인)");
        }

        session.disconnect();
        System.out.println("[DONE]");
    }
}