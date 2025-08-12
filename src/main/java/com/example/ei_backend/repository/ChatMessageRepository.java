package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.chat.ChatMessage;
import com.example.ei_backend.domain.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방의 모든 메세지(시간 순 정렬)
    List<ChatMessage> findByChatRoomOrderBySentAtAsc(ChatRoom chatRoom);

}
