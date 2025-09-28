package kr.ac.kopo.smcmfmf.example.submitservice.repository;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // 기존 메소드
    List<Assignment> findByCourse(Course course);

    // 마감일 관련 쿼리 메소드들

    /**
     * 특정 기간 사이에 마감되는 과제 조회
     */
    List<Assignment> findByCourseAndDeadlineBetween(Course course, LocalDateTime start, LocalDateTime end);

    /**
     * 마감일이 지난 과제 조회
     */
    List<Assignment> findByCourseAndDeadlineBefore(Course course, LocalDateTime dateTime);

    /**
     * 마감일이 특정 시점 이후인 과제 조회 (활성 과제)
     */
    List<Assignment> findByCourseAndDeadlineAfter(Course course, LocalDateTime dateTime);

    /**
     * 과목별 과제를 마감일 순으로 정렬하여 조회
     */
    List<Assignment> findByCourseOrderByDeadlineAsc(Course course);

    /**
     * 임박한 과제 조회 (마감일이 현재부터 지정된 시간 내)
     */
    @Query("SELECT a FROM Assignment a WHERE a.course = :course AND a.deadline > :now AND a.deadline <= :threshold ORDER BY a.deadline ASC")
    List<Assignment> findUpcomingAssignments(@Param("course") Course course,
                                             @Param("now") LocalDateTime now,
                                             @Param("threshold") LocalDateTime threshold);

    /**
     * 활성 과제 수 조회
     */
    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.course = :course AND a.deadline > :now")
    long countActiveAssignments(@Param("course") Course course, @Param("now") LocalDateTime now);

    /**
     * 마감된 과제 수 조회
     */
    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.course = :course AND a.deadline <= :now")
    long countExpiredAssignments(@Param("course") Course course, @Param("now") LocalDateTime now);

    /**
     * 마감일이 가장 가까운 과제 조회
     */
    @Query("SELECT a FROM Assignment a WHERE a.course = :course AND a.deadline > :now ORDER BY a.deadline ASC")
    List<Assignment> findNextDeadlineAssignment(@Param("course") Course course, @Param("now") LocalDateTime now);

    // 삭제 관련 쿼리 추가
    @Modifying
    @Transactional
    @Query("DELETE FROM Submission s WHERE s.assignment.assignmentId = :assignmentId")
    void deleteSubmissionsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.assignmentId = :assignmentId")
    long countSubmissionsByAssignmentId(@Param("assignmentId") Long assignmentId);
}