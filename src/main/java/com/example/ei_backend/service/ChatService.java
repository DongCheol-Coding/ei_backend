package com.example.ei_backend.service;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.domain.entity.chat.ChatMessage;
import com.example.ei_backend.domain.entity.chat.ChatRoom;
import com.example.ei_backend.exception.CustomException;
import com.example.ei_backend.exception.ErrorCode;
import com.example.ei_backend.repository.ChatMessageRepository;
import com.example.ei_backend.repository.ChatRoomRepository;
import com.example.ei_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * 멤버 - 상담자 1:1 채팅방 조회 또는 생성
     */
    @Transactional
    public Long openRoom(String memberEmail, String supportEmail) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User support = userRepository.findByEmail(supportEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        //같은 조합의 중복 채팅방 방지
        return chatRoomRepository.findByMemberAndSupport(member, support)
                .map(ChatRoom::getId)
                .orElseGet(() -> {
                    ChatRoom room = ChatRoom.builder()
                            .member(member)
                            .support(support)
                            .build();
                    return chatRoomRepository.save(room).getId();
                });
    }

    /**
     * 채팅방세 메시지 저장 + 반환
     */
    @Transactional
    public ChatMessage sendMessage(Long chatRoomId, String senderEmail, String message) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 권한 체크: 방의 멤버 또는 상담자만 보낼 수 있음
        validateParticipant(room, sender);

        ChatMessage saved = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();

        return chatMessageRepository.save(saved);
    }

    /**
     * 채팅방 메시지 전체 조회(오래된 순)
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long chatRoomId, String requestEmail) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        User requester = userRepository.findByEmail(requestEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateParticipant(room, requester);

        return chatMessageRepository.findByChatRoomOrderBySentAtAsc(room);
    }

    /**
     * 상담자용: 내가 담당 중인 채팅방 목록
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getRoomsForSupport(String supportEmail) {
        User support = userRepository.findByEmail(supportEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return chatRoomRepository.findAllBySupport(support);
    }

    /**
     * 맴버용: 내가 가진 채팅방 목록
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getRoomsForMember(String memberEmail) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return chatRoomRepository.findAllByMember(member);
    }

    private void validateParticipant(ChatRoom room, User user) {
        boolean isMember = room.getMember().getId().equals(user.getId());
        boolean isSupport = room.getSupport().getId().equals(user.getId());

        if (!(isMember || isSupport)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

}
