package kr.ac.kopo.smcmfmf.example.submitservice.service;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Submission;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubmissionService {
    private final SubmissionRepository submissionRepository;

    public Submission submitAssignment(Assignment assignment, User student, String fileUrl) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student)
                .map(existing -> {
                    try {
                        existing.resubmit(fileUrl);
                        log.info("과제 재제출 완료: 학생={}, 과제={}", student.getName(), assignment.getTitle());
                        return submissionRepository.save(existing);
                    } catch (IllegalStateException e) {
                        log.warn("재제출 실패: {}", e.getMessage());
                        throw e;
                    }
                })
                .orElseGet(() -> {
                    Submission submission = Submission.builder()
                            .assignment(assignment)
                            .student(student)
                            .fileUrl(fileUrl)
                            .build();
                    log.info("과제 제출 완료: 학생={}, 과제={}", student.getName(), assignment.getTitle());
                    return submissionRepository.save(submission);
                });
    }

    public List<Submission> getSubmissionsByAssignment(Assignment assignment) {
        return submissionRepository.findByAssignment(assignment);
    }

    // 임시 점수 저장 (아직 평가 완료되지 않음)
    public Submission updateGradeAndFeedback(Long submissionId, BigDecimal grade, String feedback) {
        Submission submission = getSubmissionById(submissionId);

        if (Boolean.TRUE.equals(submission.getIsGraded())) {
            throw new IllegalStateException("이미 평가가 완료된 과제는 수정할 수 없습니다.");
        }

        submission.updateGrading(grade, feedback);
        log.info("점수 업데이트: 제출물ID={}, 점수={}", submissionId, grade);
        return submissionRepository.save(submission);
    }

    // 평가 완료 처리
    public Submission completeGrading(Long submissionId, BigDecimal grade, String feedback) {
        Submission submission = getSubmissionById(submissionId);

        if (Boolean.TRUE.equals(submission.getIsGraded())) {
            throw new IllegalStateException("이미 평가가 완료된 과제입니다.");
        }

        submission.completeGrading(grade, feedback);
        log.info("평가 완료: 제출물ID={}, 점수={}, 학생={}",
                submissionId, grade, submission.getStudent().getName());
        return submissionRepository.save(submission);
    }

    // 평가 완료 취소 (관리자 기능)
    public Submission cancelGradingCompletion(Long submissionId) {
        Submission submission = getSubmissionById(submissionId);

        if (!Boolean.TRUE.equals(submission.getIsGraded())) {
            throw new IllegalStateException("평가 완료되지 않은 과제입니다.");
        }

        submission.setIsGraded(false);
        submission.setGradedAt(null);
        log.info("평가 완료 취소: 제출물ID={}", submissionId);
        return submissionRepository.save(submission);
    }

    // 기존 메소드들
    public Submission getSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + submissionId));
    }

    public List<Submission> getSubmissionsByStudent(User student) {
        return submissionRepository.findByStudent(student);
    }

    public Optional<Submission> getSubmissionByAssignmentAndStudent(Assignment assignment, User student) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student);
    }

    // 평가 가능 여부 확인
    public boolean canModifyGrade(Long submissionId) {
        Submission submission = getSubmissionById(submissionId);
        return !Boolean.TRUE.equals(submission.getIsGraded());
    }

    // 재제출 가능 여부 확인
    public boolean canResubmit(Assignment assignment, User student) {
        Optional<Submission> submission = getSubmissionByAssignmentAndStudent(assignment, student);
        return submission.isEmpty() || !Boolean.TRUE.equals(submission.get().getIsGraded());
    }

    @Deprecated
    public Submission gradeSubmission(Submission submission, BigDecimal grade, String feedback) {
        // 기존 호환성을 위해 유지하되, 새로운 메소드 사용을 권장
        log.warn("gradeSubmission() is deprecated. Use updateGradeAndFeedback() or completeGrading() instead.");
        return updateGradeAndFeedback(submission.getSubmissionId(), grade, feedback);
    }
}