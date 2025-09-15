package kr.ac.kopo.smcmfmf.example.submitservice.service;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Submission;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;

    public Submission submitAssignment(Assignment assignment, User student, String fileUrl) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student)
                .map(existing -> {
                    existing.setFileUrl(fileUrl);
                    // @UpdateTimestamp가 자동으로 처리하므로 직접 설정 불필요
                    return submissionRepository.save(existing);
                })
                .orElseGet(() -> {
                    Submission submission = Submission.builder()
                            .assignment(assignment)
                            .student(student)
                            .fileUrl(fileUrl)
                            .build();
                    return submissionRepository.save(submission);
                });
    }

    public List<Submission> getSubmissionsByAssignment(Assignment assignment) {
        return submissionRepository.findByAssignment(assignment);
    }

    // BigDecimal 타입으로 변경
    public Submission gradeSubmission(Submission submission, BigDecimal grade, String feedback) {
        submission.setGrade(grade);
        submission.setFeedback(feedback);
        return submissionRepository.save(submission);
    }

    // 추가 메소드들
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

    public Submission updateGradeAndFeedback(Long submissionId, BigDecimal grade, String feedback) {
        Submission submission = getSubmissionById(submissionId);
        submission.setGrade(grade);
        submission.setFeedback(feedback);
        return submissionRepository.save(submission);
    }
}