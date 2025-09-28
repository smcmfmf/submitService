package kr.ac.kopo.smcmfmf.example.submitservice.service;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Enrollment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.AssignmentRepository;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.CourseRepository;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.EnrollmentRepository;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    // 기존 메소드들...
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    public List<Course> getProfessorCourses(User professor) {
        return courseRepository.findByProfessor(professor);
    }

    public boolean enrollStudent(User student, String courseCode) {
        Course course = courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid course code"));

        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            return false; // 이미 수강 중
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .build();
        enrollmentRepository.save(enrollment);
        return true;
    }

    public List<Course> getStudentCourses(User student) {
        return enrollmentRepository.findByStudent(student)
                .stream()
                .map(Enrollment::getCourse)
                .toList();
    }

    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    /**
     * 과목 삭제 (단계별로 안전하게 삭제)
     */
    @Transactional
    public void deleteCourse(Long courseId, User professor) {
        log.info("===== 과목 삭제 프로세스 시작 =====");
        log.info("courseId: {}, professor: {}", courseId, professor.getName());

        Course course = getCourseById(courseId);

        // 권한 확인
        if (!course.getProfessor().getId().equals(professor.getId())) {
            log.error("권한 없음: courseId={}, requestProfessor={}, courseProfessor={}",
                    courseId, professor.getId(), course.getProfessor().getId());
            throw new IllegalStateException("해당 과목을 삭제할 권한이 없습니다.");
        }

        log.info("과목 삭제 시작: '{}'", course.getName());

        try {
            // 단계 1: 제출물부터 삭제 (가장 하위 레벨)
            log.info("1단계: 제출물 삭제");
            courseRepository.deleteSubmissionsByCourseId(courseId);

            // 단계 2: 과제 삭제
            log.info("2단계: 과제 삭제");
            courseRepository.deleteAssignmentsByCourseId(courseId);

            // 단계 3: 수강신청 삭제
            log.info("3단계: 수강신청 삭제");
            courseRepository.deleteEnrollmentsByCourseId(courseId);

            // 단계 4: 과목 삭제
            log.info("4단계: 과목 삭제");
            courseRepository.deleteById(courseId);
            courseRepository.flush(); // 즉시 DB 반영

            log.info("===== 과목 삭제 완료: '{}' =====", course.getName());

        } catch (Exception e) {
            log.error("과목 삭제 중 오류 발생: ", e);
            throw new RuntimeException("과목 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public CourseDeleteInfo getCourseDeleteInfo(Long courseId) {
        Course course = getCourseById(courseId);

        long assignmentCount = courseRepository.countAssignmentsByCourseId(courseId);
        long enrollmentCount = courseRepository.countEnrollmentsByCourseId(courseId);
        long submissionCount = courseRepository.countSubmissionsByCourseId(courseId);

        log.info("과목 '{}' 삭제 정보 - 과제: {}, 수강신청: {}, 제출물: {}",
                course.getName(), assignmentCount, enrollmentCount, submissionCount);

        return CourseDeleteInfo.builder()
                .courseName(course.getName())
                .assignmentCount((int)assignmentCount)
                .enrollmentCount((int)enrollmentCount)
                .submissionCount((int)submissionCount)
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class CourseDeleteInfo {
        private String courseName;
        private int assignmentCount;
        private int enrollmentCount;
        private int submissionCount;
    }
}