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
        log.info("과제 제출 처리: assignment={}, student={}", assignment.getTitle(), student.getName());

        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentAndStudent(assignment, student);

        if (existingSubmission.isPresent()) {
            // 기존 제출물이 있는 경우 - 재제출
            Submission submission = existingSubmission.get();

            // 평가가 완료된 경우 재제출 불가
            if (submission.getIsGraded()) {
                throw new IllegalStateException("이미 평가가 완료된 과제입니다. 재제출할 수 없습니다.");
            }

            // 재제출 처리
            submission.resubmit(fileUrl);
            log.info("과제 재제출 완료: {}", assignment.getTitle());
            return submissionRepository.save(submission);
        } else {
            // 새로운 제출물 생성
            Submission newSubmission = Submission.builder()
                    .assignment(assignment)
                    .student(student)
                    .fileUrl(fileUrl)
                    .isGraded(false)
                    .build();

            log.info("과제 새 제출 완료: {}", assignment.getTitle());
            return submissionRepository.save(newSubmission);
        }
    }

    @Transactional(readOnly = true)
    public List<Submission> getSubmissionsByAssignment(Assignment assignment) {
        return submissionRepository.findByAssignment(assignment);
    }

    @Transactional(readOnly = true)
    public Optional<Submission> getSubmissionByAssignmentAndStudent(Assignment assignment, User student) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student);
    }

    @Transactional(readOnly = true)
    public List<Submission> getSubmissionsByStudent(User student) {
        return submissionRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public Submission getSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + submissionId));
    }

    /**
     * 점수와 피드백 업데이트 (임시 저장 - 평가 미완료)
     */
    @Transactional
    public Submission updateGradeAndFeedback(Long submissionId, BigDecimal grade, String feedback) {
        log.info("점수 업데이트: submissionId={}, grade={}", submissionId, grade);

        Submission submission = getSubmissionById(submissionId);

        // 이미 평가가 완료된 경우 수정 불가
        if (submission.getIsGraded()) {
            throw new IllegalStateException("이미 평가가 완료된 과제는 수정할 수 없습니다.");
        }

        submission.updateGrading(grade, feedback);
        log.info("점수 임시 저장 완료: assignment={}, grade={}",
                submission.getAssignment().getTitle(), grade);

        return submissionRepository.save(submission);
    }

    /**
     * 평가 완료 처리
     */
    @Transactional
    public Submission completeGrading(Long submissionId, BigDecimal grade, String feedback) {
        log.info("평가 완료 처리: submissionId={}, grade={}", submissionId, grade);

        Submission submission = getSubmissionById(submissionId);

        // 이미 평가가 완료된 경우
        if (submission.getIsGraded()) {
            throw new IllegalStateException("이미 평가가 완료된 과제입니다.");
        }

        submission.completeGrading(grade, feedback);
        log.info("평가 완료: assignment={}, grade={}",
                submission.getAssignment().getTitle(), grade);

        return submissionRepository.save(submission);
    }

    /**
     * 평가 완료 취소 (관리자용)
     */
    @Transactional
    public Submission cancelGradingCompletion(Long submissionId) {
        log.info("평가 완료 취소: submissionId={}", submissionId);

        Submission submission = getSubmissionById(submissionId);

        if (!submission.getIsGraded()) {
            throw new IllegalStateException("평가가 완료되지 않은 과제입니다.");
        }

        submission.setIsGraded(false);
        submission.setGradedAt(null);

        log.info("평가 완료 취소 완료: assignment={}", submission.getAssignment().getTitle());

        return submissionRepository.save(submission);
    }

    /**
     * 재제출 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean canResubmit(Assignment assignment, User student) {
        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentAndStudent(assignment, student);

        if (existingSubmission.isEmpty()) {
            // 제출물이 없는 경우 제출 가능
            return true;
        }

        // 평가가 완료되지 않은 경우에만 재제출 가능
        return !existingSubmission.get().getIsGraded();
    }

    /**
     * 마감일 체크 (제출 시 사용)
     */
    @Transactional(readOnly = true)
    public boolean isSubmissionAllowed(Assignment assignment) {
        LocalDateTime now = LocalDateTime.now();
        return assignment.getDeadline().isAfter(now);
    }

    /**
     * 특정 학생의 특정 과목 제출물 조회 (수강 철회 시 사용)
     */
    @Transactional(readOnly = true)
    public List<Submission> getSubmissionsByStudentAndCourse(User student, Long courseId) {
        return submissionRepository.findByStudentAndCourseId(student, courseId);
    }

    /**
     * 특정 학생의 특정 과목 제출물 수 조회
     */
    @Transactional(readOnly = true)
    public long countSubmissionsByStudentAndCourse(User student, Long courseId) {
        return submissionRepository.countByStudentAndCourseId(student, courseId);
    }

    /**
     * 특정 학생의 특정 과목 평가 완료된 제출물 수 조회
     */
    @Transactional(readOnly = true)
    public long countGradedSubmissionsByStudentAndCourse(User student, Long courseId) {
        return submissionRepository.countGradedByStudentAndCourseId(student, courseId);
    }
}