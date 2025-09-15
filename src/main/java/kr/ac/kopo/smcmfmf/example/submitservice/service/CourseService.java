package kr.ac.kopo.smcmfmf.example.submitservice.service;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Enrollment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.CourseRepository;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

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
        return  courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid course id"));
    }
}
