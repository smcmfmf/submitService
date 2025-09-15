package kr.ac.kopo.smcmfmf.example.submitservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"assignment_id", "student_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
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

    @Column(nullable = false, length = 255)
    private String fileUrl;

    // DB의 ON UPDATE CURRENT_TIMESTAMP에 대응
    @UpdateTimestamp
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    // DB의 DECIMAL(5,2)에 대응 - 더 정확한 소수점 처리
    @Column(precision = 5, scale = 2)
    private BigDecimal grade;

    @Lob  // TEXT 타입에 대응
    @Column(columnDefinition = "TEXT")
    private String feedback;
}