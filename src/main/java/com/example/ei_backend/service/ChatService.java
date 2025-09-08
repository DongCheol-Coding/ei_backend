package com.example.ei_backend.service;

import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.dto.chat.ChatMessageResponseDto;
import com.example.ei_backend.domain.dto.chat.ChatRoomSummaryDto;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.domain.entity.chat.ChatMessage;
import com.example.ei_backend.domain.entity.chat.ChatRoom;
import com.example.ei_backend.exception.CustomException;
import com.example.ei_backend.exception.ErrorCode;
import com.example.ei_backend.exception.NotFoundException;
import com.example.ei_backend.repository.ChatMessageRepository;
import com.example.ei_backend.repository.ChatRoomRepository;
import com.example.ei_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // afterCommit 발행용
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 멤버 - 상담자 1:1 채팅방 조회 또는 생성
     */
    @Transactional
    public Long openRoom(String memberEmail, String supportEmail) {
        User member = userRepository.findByEmailAndIsDeletedFalse(memberEmail)
                .orElseThrow(() -> new NotFoundException("member"));

        User support = userRepository.findByEmailAndIsDeletedFalse(supportEmail)
                .filter(u -> u.getRoles().contains(UserRole.ROLE_SUPPORT))
                .orElseThrow(() -> new NotFoundException("support"));

        ChatRoom room = chatRoomRepository
                .findByMemberAndSupportAndClosedAtIsNull(member, support)
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.builder()
                                .member(member)
                                .support(support)
                                .build()));

        return room.getId();
    }

    /**
     * 채팅방에 메시지 저장 → 커밋 이후(/user/queue/messages) 발행
     */
    @Transactional
    public ChatMessage sendMessage(Long chatRoomId, String senderEmail, String message) {
        // (성능 개선 원하면) chatRoomRepository.findByIdWithUsers(...)로 교체 권장
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (room.getClosedAt() != null) {
            throw new CustomException(ErrorCode.CHAT_ROOM_CLOSED);
        }

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

        // ... 생략 ...
        saved = chatMessageRepository.save(saved);

// 트랜잭션 안에서 필요한 값들 미리 확정
        final String memberEmail = room.getMember().getEmail();
        final String supportEmail = room.getSupport().getEmail();
        final String recipientEmail = senderEmail.equals(memberEmail) ? supportEmail : memberEmail;

// DTO와 함께 "메시지 ID"도 스칼라로 캡처
        final ChatMessageResponseDto payload = ChatMessageResponseDto.from(saved);
        final Long msgId = saved.getId();   // <-- 여기!

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Transaction synchronization is not active. Ensure @Transactional is applied.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/messages", payload);
                messagingTemplate.convertAndSendToUser(senderEmail, "/queue/messages", payload);
                // DTO에 getId()가 없어도 엔티티 ID를 캡처해뒀으니 OK
                log.info("[chat] committed => to={}, from={}, msgId={}", recipientEmail, senderEmail, msgId);
            }
        });

        return saved;
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
     * 멤버용: 내가 가진 채팅방 목록
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getRoomsForMember(String memberEmail) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return chatRoomRepository.findAllByMember(member);
    }

    private void validateParticipant(ChatRoom room, User user) {
        boolean isMember  = room.getMember()  != null && room.getMember().getId().equals(user.getId());
        boolean isSupport = room.getSupport() != null && room.getSupport().getId().equals(user.getId());
        if (!(isMember || isSupport)) throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public com.example.ei_backend.domain.dto.chat.CloseRoomResponse closeRoom(
            Long roomId, String supportEmail, com.example.ei_backend.domain.dto.chat.CloseRoomRequest req) {

        ChatRoom room = chatRoomRepository.findByIdAndClosedAtIsNull(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        User support = userRepository.findByEmailAndIsDeletedFalse(supportEmail)
                .filter(u -> u.getRoles().contains(UserRole.ROLE_SUPPORT))
                .orElseThrow(() -> new CustomException(ErrorCode.ACCESS_DENIED));

        if (room.getSupport() == null || !room.getSupport().getId().equals(support.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        room.close(ChatRoom.ClosedBy.SUPPORT, (req != null ? req.getReason() : null));

        return com.example.ei_backend.domain.dto.chat.CloseRoomResponse.builder()
                .roomId(room.getId())
                .closed(true)
                .by("SUPPORT")
                .closedAt(room.getClosedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ChatRoomSummaryDto> getMyRoomsForSupport(
            String supportEmail, String status, Pageable pageable) {

        User support = userRepository.findByEmailAndIsDeletedFalse(supportEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<ChatRoom> rooms;
        switch ((status == null ? "open" : status).toLowerCase()) {
            case "closed" -> rooms = chatRoomRepository
                    .findAllBySupportIdAndClosedAtIsNotNull(support.getId(), pageable);
            case "all"    -> rooms = chatRoomRepository
                    .findAllBySupportId(support.getId(), pageable);
            default       -> rooms = chatRoomRepository
                    .findAllBySupportIdAndClosedAtIsNull(support.getId(), pageable); // open
        }
        return rooms.map(ChatRoomSummaryDto::from);
    }
}
