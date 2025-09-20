// src/main/java/kr/ac/kopo/smcmfmf/example/submitservice/domain/Submission.java

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

    // 평가 완료 여부를 나타내는 필드 추가
    @Column(nullable = false)
    @Builder.Default
    private Boolean isGraded = false;

    // 평가 완료 시점을 기록하는 필드 추가
    private LocalDateTime gradedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 평가 완료 처리 메소드
    public void completeGrading(BigDecimal grade, String feedback) {
        this.grade = grade;
        this.feedback = feedback;
        this.isGraded = true;
        this.gradedAt = LocalDateTime.now();
    }

    // 평가 수정 처리 메소드 (평가 완료 전에만 가능)
    public void updateGrading(BigDecimal grade, String feedback) {
        if (this.isGraded) {
            throw new IllegalStateException("이미 평가가 완료된 과제는 수정할 수 없습니다.");
        }
        this.grade = grade;
        this.feedback = feedback;
    }

    // 재제출 처리 메소드 (평가 완료 전에만 가능)
    public void resubmit(String newFileUrl) {
        if (this.isGraded) {
            throw new IllegalStateException("이미 평가가 완료된 과제는 재제출할 수 없습니다.");
        }
        this.fileUrl = newFileUrl;
        // 기존 점수와 피드백 초기화
        this.grade = null;
        this.feedback = null;
    }
}