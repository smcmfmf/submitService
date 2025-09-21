package kr.ac.kopo.smcmfmf.example.submitservice.service;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User registerUser(User user) {
        // 관리자가 아닌 경우 승인 대기 상태로 설정
        if (!User.Role.ADMIN.equals(user.getRole())) {
            user.setAccountStatus(User.AccountStatus.PENDING);
            log.info("새 사용자 등록 (승인 대기): {} ({})", user.getName(), user.getEmail());
        } else {
            // 관리자는 즉시 승인 상태로 설정 (첫 관리자 계정 생성용)
            user.setAccountStatus(User.AccountStatus.APPROVED);
            log.info("관리자 계정 등록: {} ({})", user.getName(), user.getEmail());
        }

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + id));
    }

    // 관리자 기능들
    public List<User> findByAccountStatus(User.AccountStatus status) {
        return userRepository.findByAccountStatusOrderByCreatedAtDesc(status);
    }

    public long countByAccountStatus(User.AccountStatus status) {
        return userRepository.countByAccountStatus(status);
    }

    public long countAllUsers() {
        return userRepository.count();
    }

    public List<User> findRecentApplications(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findRecentApplicationsByStatus(User.AccountStatus.PENDING, pageable).getContent();
    }

    public Page<User> findUsersWithFilters(String status, String role, Pageable pageable) {
        if ("ALL".equals(status) && "ALL".equals(role)) {
            return userRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else if ("ALL".equals(status)) {
            return userRepository.findByRoleOrderByCreatedAtDesc(User.Role.valueOf(role), pageable);
        } else if ("ALL".equals(role)) {
            return userRepository.findByAccountStatusOrderByCreatedAtDesc(User.AccountStatus.valueOf(status), pageable);
        } else {
            return userRepository.findByAccountStatusAndRoleOrderByCreatedAtDesc(
                    User.AccountStatus.valueOf(status), User.Role.valueOf(role), pageable);
        }
    }

    @Transactional
    public User approveUser(Long userId, User admin, String reason) {
        User user = findById(userId);

        if (!User.AccountStatus.PENDING.equals(user.getAccountStatus())) {
            throw new IllegalStateException("승인 대기 상태가 아닌 사용자입니다.");
        }

        user.approve(admin, reason);
        log.info("사용자 승인: {} by {}", user.getEmail(), admin.getEmail());
        return userRepository.save(user);
    }

    @Transactional
    public User rejectUser(Long userId, User admin, String reason) {
        User user = findById(userId);

        if (!User.AccountStatus.PENDING.equals(user.getAccountStatus())) {
            throw new IllegalStateException("승인 대기 상태가 아닌 사용자입니다.");
        }

        user.reject(admin, reason);
        log.info("사용자 거부: {} by {}", user.getEmail(), admin.getEmail());
        return userRepository.save(user);
    }

    @Transactional
    public User suspendUser(Long userId, User admin, String reason) {
        User user = findById(userId);

        if (user.isAdmin()) {
            throw new IllegalStateException("관리자 계정은 정지할 수 없습니다.");
        }

        user.suspend(admin, reason);
        log.info("사용자 정지: {} by {}", user.getEmail(), admin.getEmail());
        return userRepository.save(user);
    }

    @Transactional
    public User reactivateUser(Long userId, User admin, String reason) {
        User user = findById(userId);
        user.approve(admin, reason); // 다시 승인 상태로 변경
        log.info("사용자 재활성화: {} by {}", user.getEmail(), admin.getEmail());
        return userRepository.save(user);
    }

    // 로그인 검증 메소드
    public boolean canUserLogin(User user) {
        return user.isApproved();
    }

    // 첫 번째 관리자 계정이 있는지 확인
    public boolean hasAdminAccount() {
        return userRepository.existsByRole(User.Role.ADMIN);
    }

    // 첫 번째 관리자 계정 생성
    @Transactional
    public User createFirstAdminAccount(String name, String email, String password) {
        if (hasAdminAccount()) {
            throw new IllegalStateException("이미 관리자 계정이 존재합니다.");
        }

        User admin = User.builder()
                .name(name)
                .email(email)
                .password(password)
                .role(User.Role.ADMIN)
                .accountStatus(User.AccountStatus.APPROVED)
                .build();

        log.info("첫 번째 관리자 계정 생성: {}", email);
        return userRepository.save(admin);
    }
}