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

    // ===== 기존 =====
    Optional<ChatRoom> findByMemberAndSupport(User member, User support);
    List<ChatRoom> findAllBySupport(User support);
    List<ChatRoom> findAllByMember(User member);
    Page<ChatRoom> findAllBySupportId(Long supportId, Pageable pageable);
    Page<ChatRoom> findAllByMemberId(Long memberId, Pageable pageable);

    @Query("""
    select r
    from ChatRoom r
    join fetch r.member m
    join fetch r.support s
    where s.id = :supportId
""")
    List<ChatRoom> findWithUsersBySupportId(@Param("supportId") Long supportId);

    // ===== 신규: "열린 방만" 중복 생성 방지용 =====
    Optional<ChatRoom> findByMemberAndSupportAndClosedAtIsNull(User member, User support);

    // ===== 신규: 상태별 목록/페이지 =====
    List<ChatRoom> findAllBySupportIdAndClosedAtIsNull(Long supportId);
    List<ChatRoom> findAllByMemberIdAndClosedAtIsNull(Long memberId);

    Page<ChatRoom> findAllBySupportIdAndClosedAtIsNull(Long supportId, Pageable pageable);
    Page<ChatRoom> findAllByMemberIdAndClosedAtIsNull(Long memberId, Pageable pageable);

    // ===== 신규: 열린 방 단건 조회 (서비스에서 close 전에 검증 용) =====
    Optional<ChatRoom> findByIdAndClosedAtIsNull(Long id);

    // ===== (옵션) 통계/필터링에 유용 =====
    long countBySupportIdAndClosedAtIsNull(Long supportId);
    long countByMemberIdAndClosedAtIsNull(Long memberId);
}
