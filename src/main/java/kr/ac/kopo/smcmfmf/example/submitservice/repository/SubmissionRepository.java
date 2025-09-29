package kr.ac.kopo.smcmfmf.example.submitservice.repository;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Submission;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignment(Assignment assignment);
    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);
    List<Submission> findByStudent(User student);

    // 학생의 특정 과목 모든 제출물 조회
    @Query("SELECT s FROM Submission s WHERE s.student = :student AND s.assignment.course.courseId = :courseId")
    List<Submission> findByStudentAndCourseId(@Param("student") User student, @Param("courseId") Long courseId);

    // 학생의 특정 과목 제출물 수 조회
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.student = :student AND s.assignment.course.courseId = :courseId")
    long countByStudentAndCourseId(@Param("student") User student, @Param("courseId") Long courseId);

    // 학생의 특정 과목 평가 완료된 제출물 수 조회
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.student = :student AND s.assignment.course.courseId = :courseId AND s.isGraded = true")
    long countGradedByStudentAndCourseId(@Param("student") User student, @Param("courseId") Long courseId);
}