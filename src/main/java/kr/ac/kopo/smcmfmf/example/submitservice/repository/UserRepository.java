package kr.ac.kopo.smcmfmf.example.submitservice.repository;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 기존 메소드
    Optional<User> findByEmail(String email);

    // 계정 상태별 조회
    List<User> findByAccountStatusOrderByCreatedAtDesc(User.AccountStatus status);
    long countByAccountStatus(User.AccountStatus status);

    // 역할별 조회
    Page<User> findByRoleOrderByCreatedAtDesc(User.Role role, Pageable pageable);
    boolean existsByRole(User.Role role);

    // 복합 조건 조회
    Page<User> findByAccountStatusOrderByCreatedAtDesc(User.AccountStatus status, Pageable pageable);
    Page<User> findByAccountStatusAndRoleOrderByCreatedAtDesc(User.AccountStatus status, User.Role role, Pageable pageable);
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 최근 신청자 조회 (최대 5개) - H2 호환성을 위해 Pageable 사용
    @Query("SELECT u FROM User u WHERE u.accountStatus = :status ORDER BY u.createdAt DESC")
    Page<User> findRecentApplicationsByStatus(User.AccountStatus status, Pageable pageable);

    // 승인된 사용자만 조회 (기존 기능 유지용)
    List<User> findByAccountStatus(User.AccountStatus status);
}