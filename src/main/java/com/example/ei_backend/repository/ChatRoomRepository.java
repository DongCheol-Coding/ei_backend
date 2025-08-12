package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.domain.entity.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {


    // 한 멤버와 상담자의 방은 1개만 존재 (중복 생성 방지)
    Optional<ChatRoom> findByMemberAndSupport(User member, User support);

    // 상담자가 가진 전체 채팅방 목록
    List<ChatRoom> findAllBySupport(User support);

    // 맴버가 가진 채팅방
    List<ChatRoom> findAllByMember(User member);


    // 페이지 네이션
    Page<ChatRoom> findAllBySupportId(Long supportId, Pageable pageable);
    Page<ChatRoom> findAllByMemberId(Long memberId, Pageable pageable);


    // 지연로딩 최적화
    @Query("select r from ChatRoom r " +
            "join fetch r.member m " +
            "join fetch r.support s " +
            "where s.id = :supportId")
    List<ChatRoom> findWithUsersBySupportId(@Param("supportId") Long supportId);

}
