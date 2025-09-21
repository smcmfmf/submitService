package kr.ac.kopo.smcmfmf.example.submitservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 계정 승인 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING;

    // 승인자 정보 (관리자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // 승인일시
    private LocalDateTime approvedAt;

    // 계정 생성일시
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 승인 사유 또는 거부 사유
    @Column(length = 500)
    private String statusReason;

    public enum Role {
        STUDENT("학생"),
        PROFESSOR("교수"),
        ADMIN("관리자");

        private final String displayName;

        Role(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AccountStatus {
        PENDING("승인 대기"),
        APPROVED("승인됨"),
        REJECTED("거부됨"),
        SUSPENDED("정지됨");

        private final String displayName;

        AccountStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 편의 메소드들
    public boolean isApproved() {
        return AccountStatus.APPROVED.equals(this.accountStatus);
    }

    public boolean isPending() {
        return AccountStatus.PENDING.equals(this.accountStatus);
    }

    public boolean isRejected() {
        return AccountStatus.REJECTED.equals(this.accountStatus);
    }

    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    public boolean isStudent() {
        return Role.STUDENT.equals(this.role);
    }

    public boolean isProfessor() {
        return Role.PROFESSOR.equals(this.role);
    }

    public void approve(User admin, String reason) {
        this.accountStatus = AccountStatus.APPROVED;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
        this.statusReason = reason;
    }

    public void reject(User admin, String reason) {
        this.accountStatus = AccountStatus.REJECTED;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
        this.statusReason = reason;
    }

    public void suspend(User admin, String reason) {
        this.accountStatus = AccountStatus.SUSPENDED;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
        this.statusReason = reason;
    }
}