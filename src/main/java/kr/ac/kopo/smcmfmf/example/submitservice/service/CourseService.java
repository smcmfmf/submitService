package kr.ac.kopo.smcmfmf.example.submitservice.service;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Enrollment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Submission;
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

    // 과목 생성
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    /**
     * 교수의 과목 조회 - 생성일 기준 내림차순 정렬 (최신순)
     */
    public List<Course> getProfessorCourses(User professor) {
        log.debug("교수 {} 의 과목 조회 (최신순 정렬)", professor.getName());
        return courseRepository.findByProfessorOrderByCreatedAtDesc(professor);
    }

    /**
     * 교수의 과목 조회 - 생성일 기준 오름차순 정렬 (오래된 순)
     */
    public List<Course> getProfessorCoursesOldestFirst(User professor) {
        log.debug("교수 {} 의 과목 조회 (오래된 순 정렬)", professor.getName());
        return courseRepository.findByProfessorOrderByCreatedAtAsc(professor);
    }

    /**
     * 학생의 수강 과목 조회 - 생성일 기준 내림차순 정렬 (최신순)
     */
    public List<Course> getStudentCourses(User student) {
        log.debug("학생 {} 의 수강 과목 조회 (최신순 정렬)", student.getName());
        return courseRepository.findStudentCoursesOrderByCreatedAtDesc(student);
    }

    /**
     * 학생의 수강 과목 조회 - 생성일 기준 오름차순 정렬 (오래된 순)
     */
    public List<Course> getStudentCoursesOldestFirst(User student) {
        log.debug("학생 {} 의 수강 과목 조회 (오래된 순 정렬)", student.getName());
        return courseRepository.findStudentCoursesOrderByCreatedAtAsc(student);
    }

    /**
     * 수강 신청 처리
     */
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
        log.info("학생 수강 신청 완료: {} -> {}", student.getName(), course.getName());
        return true;
    }

    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    /**
     * 학생이 특정 과목을 수강하고 있는지 확인
     */
    public boolean isStudentEnrolled(User student, Course course) {
        return enrollmentRepository.existsByStudentAndCourse(student, course);
    }

    /**
     * 수강 철회 정보 조회
     */
    @Transactional(readOnly = true)
    public CourseWithdrawInfo getCourseWithdrawInfo(User student, Course course) {
        log.info("수강 철회 정보 조회: student={}, course={}", student.getName(), course.getName());

        // 과제 목록 조회
        List<Assignment> assignments = assignmentRepository.findByCourse(course);

        // 학생의 제출물 정보 수집 - 새로운 메소드 사용
        int totalSubmissions = (int) submissionRepository.countByStudentAndCourseId(student, course.getCourseId());
        int gradedSubmissions = (int) submissionRepository.countGradedByStudentAndCourseId(student, course.getCourseId());

        CourseWithdrawInfo info = CourseWithdrawInfo.builder()
                .courseName(course.getName())
                .courseCode(course.getCode())
                .totalAssignments(assignments.size())
                .submittedAssignments(totalSubmissions)
                .gradedSubmissions(gradedSubmissions)
                .hasGradedSubmissions(gradedSubmissions > 0)
                .build();

        log.info("철회 정보: 전체과제={}, 제출과제={}, 평가완료={}",
                info.getTotalAssignments(), info.getSubmittedAssignments(), info.getGradedSubmissions());

        return info;
    }

    /**
     * 수강 철회 실행
     */
    @Transactional
    public CourseWithdrawResult withdrawStudent(User student, Course course) {
        log.info("===== 수강 철회 프로세스 시작 =====");
        log.info("student: {}, course: {}", student.getName(), course.getName());

        // 수강 여부 확인
        if (!isStudentEnrolled(student, course)) {
            throw new IllegalStateException("수강하지 않은 과목입니다.");
        }

        try {
            // 1단계: 해당 학생의 제출물 삭제
            int deletedSubmissions = deleteStudentSubmissions(student, course);
            log.info("제출물 삭제 완료: {} 개", deletedSubmissions);

            // 2단계: 수강신청 삭제
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
            Enrollment targetEnrollment = enrollments.stream()
                    .filter(e -> e.getCourse().getCourseId().equals(course.getCourseId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("수강신청 정보를 찾을 수 없습니다."));

            enrollmentRepository.delete(targetEnrollment);
            enrollmentRepository.flush(); // 즉시 DB 반영
            log.info("수강신청 삭제 완료");

            CourseWithdrawResult result = CourseWithdrawResult.builder()
                    .courseName(course.getName())
                    .deletedSubmissions(deletedSubmissions)
                    .success(true)
                    .build();

            log.info("===== 수강 철회 완료: {} - {} =====", student.getName(), course.getName());
            return result;

        } catch (Exception e) {
            log.error("수강 철회 중 오류 발생: ", e);
            throw new RuntimeException("수강 철회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 학생의 특정 과목 제출물 모두 삭제
     */
    private int deleteStudentSubmissions(User student, Course course) {
        List<Submission> submissions = submissionRepository.findByStudentAndCourseId(student, course.getCourseId());
        int deletedCount = submissions.size();

        if (!submissions.isEmpty()) {
            submissionRepository.deleteAll(submissions);
            submissionRepository.flush(); // 즉시 DB 반영
            log.info("학생 {} 의 과목 {} 제출물 {} 개 삭제 완료",
                    student.getName(), course.getName(), deletedCount);
        }

        return deletedCount;
    }

    /**
     * 과목 삭제 (단계별로 안전하게 삭제) - 교수용
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

    // 데이터 클래스들
    @lombok.Builder
    @lombok.Data
    public static class CourseDeleteInfo {
        private String courseName;
        private int assignmentCount;
        private int enrollmentCount;
        private int submissionCount;
    }

    @lombok.Builder
    @lombok.Data
    public static class CourseWithdrawInfo {
        private String courseName;
        private String courseCode;
        private int totalAssignments;
        private int submittedAssignments;
        private int gradedSubmissions;
        private boolean hasGradedSubmissions;
    }

    @lombok.Builder
    @lombok.Data
    public static class CourseWithdrawResult {
        private String courseName;
        private int deletedSubmissions;
        private boolean success;
    }
}