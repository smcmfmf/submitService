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
public class SubmissionService {
    private final SubmissionRepository submissionRepository;

    @Transactional
    public Submission submitAssignment(Assignment assignment, User student, String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 URL은 필수입니다.");
        }

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
                            .isGraded(Boolean.FALSE) // 명시적으로 설정
                            .build();
                    log.info("과제 제출 완료: 학생={}, 과제={}", student.getName(), assignment.getTitle());
                    return submissionRepository.save(submission);
                });
    }

    @Transactional(readOnly = true)
    public List<Submission> getSubmissionsByAssignment(Assignment assignment) {
        try {
            List<Submission> submissions = submissionRepository.findByAssignment(assignment);
            log.debug("과제 '{}' 제출물 {} 개 조회", assignment.getTitle(), submissions.size());
            return submissions;
        } catch (Exception e) {
            log.error("제출물 목록 조회 중 오류 발생: assignment={}", assignment.getTitle(), e);
            throw new RuntimeException("제출물 목록을 불러올 수 없습니다.", e);
        }
    }

    @Transactional
    public Submission updateGradeAndFeedback(Long submissionId, BigDecimal grade, String feedback) {
        if (submissionId == null) {
            throw new IllegalArgumentException("제출물 ID는 필수입니다.");
        }
        if (grade == null) {
            throw new IllegalArgumentException("점수는 필수 입력 항목입니다.");
        }

        try {
            Submission submission = getSubmissionById(submissionId);

            if (Boolean.TRUE.equals(submission.getIsGraded())) {
                throw new IllegalStateException("이미 평가가 완료된 과제는 수정할 수 없습니다.");
            }

            submission.updateGrading(grade, feedback);
            log.info("점수 업데이트: 제출물ID={}, 점수={}", submissionId, grade);
            return submissionRepository.save(submission);
        } catch (Exception e) {
            log.error("점수 업데이트 중 오류 발생: submissionId={}", submissionId, e);
            throw e;
        }
    }

    @Transactional
    public Submission completeGrading(Long submissionId, BigDecimal grade, String feedback) {
        if (submissionId == null) {
            throw new IllegalArgumentException("제출물 ID는 필수입니다.");
        }
        if (grade == null) {
            throw new IllegalArgumentException("점수는 필수 입력 항목입니다.");
        }

        try {
            Submission submission = getSubmissionById(submissionId);

            if (Boolean.TRUE.equals(submission.getIsGraded())) {
                throw new IllegalStateException("이미 평가가 완료된 과제입니다.");
            }

            submission.completeGrading(grade, feedback);
            log.info("평가 완료: 제출물ID={}, 점수={}, 학생={}",
                    submissionId, grade, submission.getStudent().getName());
            return submissionRepository.save(submission);
        } catch (Exception e) {
            log.error("평가 완료 중 오류 발생: submissionId={}", submissionId, e);
            throw e;
        }
    }

    @Transactional
    public Submission cancelGradingCompletion(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("제출물 ID는 필수입니다.");
        }

        try {
            Submission submission = getSubmissionById(submissionId);

            if (!Boolean.TRUE.equals(submission.getIsGraded())) {
                throw new IllegalStateException("평가 완료되지 않은 과제입니다.");
            }

            submission.setIsGraded(Boolean.FALSE);
            submission.setGradedAt(null);
            log.info("평가 완료 취소: 제출물ID={}", submissionId);
            return submissionRepository.save(submission);
        } catch (Exception e) {
            log.error("평가 완료 취소 중 오류 발생: submissionId={}", submissionId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Submission getSubmissionById(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("제출물 ID는 필수입니다.");
        }

        try {
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("제출물을 찾을 수 없습니다: " + submissionId));

            // isGraded 필드 초기화 확인
            if (submission.getIsGraded() == null) {
                log.warn("isGraded 필드가 null인 제출물 발견: {}", submissionId);
                submission.setIsGraded(Boolean.FALSE);
                submissionRepository.save(submission);
            }

            return submission;
        } catch (Exception e) {
            log.error("제출물 조회 중 오류 발생: submissionId={}", submissionId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<Submission> getSubmissionsByStudent(User student) {
        if (student == null) {
            throw new IllegalArgumentException("학생 정보는 필수입니다.");
        }

        try {
            return submissionRepository.findByStudent(student);
        } catch (Exception e) {
            log.error("학생 제출물 목록 조회 중 오류 발생: student={}", student.getName(), e);
            throw new RuntimeException("제출물 목록을 불러올 수 없습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Submission> getSubmissionByAssignmentAndStudent(Assignment assignment, User student) {
        if (assignment == null || student == null) {
            throw new IllegalArgumentException("과제와 학생 정보는 필수입니다.");
        }

        try {
            return submissionRepository.findByAssignmentAndStudent(assignment, student);
        } catch (Exception e) {
            log.error("특정 과제 제출물 조회 중 오류 발생: assignment={}, student={}",
                    assignment.getTitle(), student.getName(), e);
            throw new RuntimeException("제출물을 불러올 수 없습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public boolean canModifyGrade(Long submissionId) {
        try {
            Submission submission = getSubmissionById(submissionId);
            return !Boolean.TRUE.equals(submission.getIsGraded());
        } catch (Exception e) {
            log.error("평가 수정 가능 여부 확인 중 오류 발생: submissionId={}", submissionId, e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canResubmit(Assignment assignment, User student) {
        try {
            Optional<Submission> submission = getSubmissionByAssignmentAndStudent(assignment, student);
            return submission.isEmpty() || !Boolean.TRUE.equals(submission.get().getIsGraded());
        } catch (Exception e) {
            log.error("재제출 가능 여부 확인 중 오류 발생: assignment={}, student={}",
                    assignment.getTitle(), student.getName(), e);
            return false;
        }
    }

    // 기존 호환성을 위한 메소드 (사용 권장하지 않음)
    @Deprecated
    public Submission gradeSubmission(Submission submission, BigDecimal grade, String feedback) {
        log.warn("gradeSubmission() is deprecated. Use updateGradeAndFeedback() or completeGrading() instead.");
        return updateGradeAndFeedback(submission.getSubmissionId(), grade, feedback);
    }
}