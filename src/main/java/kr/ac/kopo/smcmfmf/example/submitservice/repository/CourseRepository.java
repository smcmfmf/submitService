package kr.ac.kopo.smcmfmf.example.submitservice.repository;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 기존 메소드 (정렬 없음)
    List<Course> findByProfessor(User professor);

    // 교수의 과목 조회 - 생성일 기준 내림차순 정렬 (최신순)
    List<Course> findByProfessorOrderByCreatedAtDesc(User professor);

    // 교수의 과목 조회 - 생성일 기준 오름차순 정렬 (오래된 순)
    List<Course> findByProfessorOrderByCreatedAtAsc(User professor);

    Optional<Course> findByCode(String code); // 학생이 join할 때 과목 코드로 조회

    // 학생 수강 과목 조회 - 생성일 기준 내림차순 정렬
    @Query("SELECT c FROM Course c " +
            "JOIN Enrollment e ON e.course = c " +
            "WHERE e.student = :student " +
            "ORDER BY c.createdAt DESC")
    List<Course> findStudentCoursesOrderByCreatedAtDesc(@Param("student") User student);

    // 학생 수강 과목 조회 - 생성일 기준 오름차순 정렬
    @Query("SELECT c FROM Course c " +
            "JOIN Enrollment e ON e.course = c " +
            "WHERE e.student = :student " +
            "ORDER BY c.createdAt ASC")
    List<Course> findStudentCoursesOrderByCreatedAtAsc(@Param("student") User student);

    // 과목 삭제를 위한 통계 쿼리들
    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.course.courseId = :courseId")
    long countAssignmentsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.courseId = :courseId")
    long countEnrollmentsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.course.courseId = :courseId")
    long countSubmissionsByCourseId(@Param("courseId") Long courseId);

    // 강제 삭제용 쿼리들 (CASCADE가 작동하지 않을 때 사용)
    @Modifying
    @Transactional
    @Query("DELETE FROM Submission s WHERE s.assignment.course.courseId = :courseId")
    void deleteSubmissionsByCourseId(@Param("courseId") Long courseId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Assignment a WHERE a.course.courseId = :courseId")
    void deleteAssignmentsByCourseId(@Param("courseId") Long courseId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Enrollment e WHERE e.course.courseId = :courseId")
    void deleteEnrollmentsByCourseId(@Param("courseId") Long courseId);
}