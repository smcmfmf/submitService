package kr.ac.kopo.smcmfmf.example.submitservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private String fileUrl;

    @Column(precision = 5, scale = 2)
    private BigDecimal grade;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    // 평가 완료 여부를 나타내는 필드 - 기본값을 명시적으로 설정
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isGraded = Boolean.FALSE;

    // 평가 완료 시점을 기록하는 필드
    private LocalDateTime gradedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Boolean 타입에 대한 getter 메소드를 안전하게 처리
    public Boolean getIsGraded() {
        return this.isGraded != null ? this.isGraded : Boolean.FALSE;
    }

    public void setIsGraded(Boolean isGraded) {
        this.isGraded = isGraded != null ? isGraded : Boolean.FALSE;
    }

    // Thymeleaf에서 사용할 수 있도록 boolean primitive 타입 getter 추가
    public boolean isGraded() {
        return Boolean.TRUE.equals(this.getIsGraded());
    }

    // Lombok @Data가 자동 생성하는 메소드들과 충돌하지 않도록 명시적으로 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Submission that = (Submission) o;
        return Objects.equals(submissionId, that.submissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submissionId);
    }

    // 평가 완료 처리 메소드
    public void completeGrading(BigDecimal grade, String feedback) {
        if (grade == null) {
            throw new IllegalArgumentException("점수는 필수 입력 항목입니다.");
        }

        this.grade = grade;
        this.feedback = feedback;
        this.isGraded = Boolean.TRUE;
        this.gradedAt = LocalDateTime.now();
    }

    // 평가 수정 처리 메소드 (평가 완료 전에만 가능)
    public void updateGrading(BigDecimal grade, String feedback) {
        if (Boolean.TRUE.equals(this.getIsGraded())) {
            throw new IllegalStateException("이미 평가가 완료된 과제는 수정할 수 없습니다.");
        }

        if (grade == null) {
            throw new IllegalArgumentException("점수는 필수 입력 항목입니다.");
        }

        this.grade = grade;
        this.feedback = feedback;
    }

    // 재제출 처리 메소드 (평가 완료 전에만 가능)
    public void resubmit(String newFileUrl) {
        if (Boolean.TRUE.equals(this.getIsGraded())) {
            throw new IllegalStateException("이미 평가가 완료된 과제는 재제출할 수 없습니다.");
        }

        if (newFileUrl == null || newFileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 URL은 필수입니다.");
        }

        this.fileUrl = newFileUrl;
        // 기존 점수와 피드백 초기화
        this.grade = null;
        this.feedback = null;
    }

    // 편의 메소드들 - null 체크를 강화
    public boolean isGradingCompleted() {
        return Boolean.TRUE.equals(this.getIsGraded());
    }

    public boolean hasGrade() {
        return this.grade != null;
    }

    public boolean hasFeedback() {
        return this.feedback != null && !this.feedback.trim().isEmpty();
    }

    // toString 메소드에서 Lazy Loading 문제 방지
    @Override
    public String toString() {
        return "Submission{" +
                "submissionId=" + submissionId +
                ", fileUrl='" + fileUrl + '\'' +
                ", grade=" + grade +
                ", isGraded=" + isGraded +
                ", submittedAt=" + submittedAt +
                '}';
    }
}