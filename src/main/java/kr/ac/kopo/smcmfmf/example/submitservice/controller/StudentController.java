package kr.ac.kopo.smcmfmf.example.submitservice.controller;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Submission;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.service.AssignmentService;
import kr.ac.kopo.smcmfmf.example.submitservice.service.CourseService;
import kr.ac.kopo.smcmfmf.example.submitservice.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @GetMapping("/dashboard")
    public String dashboard(@SessionAttribute("user") User user, Model model) {
        List<Course> courses = courseService.getStudentCourses(user);
        model.addAttribute("courses", courses);
        return "student/dashboard";
    }

    // 수강 신청 폼
    @GetMapping("/enroll")
    public String enrollForm() {
        return "student/enroll_form";
    }

    // 수강 신청 처리
    @PostMapping("/enroll")
    public String enrollCourse(@RequestParam String courseCode,
                               @SessionAttribute("user") User student,
                               Model model) {
        try {
            boolean success = courseService.enrollStudent(student, courseCode);
            if (success) {
                return "redirect:/student/dashboard";
            } else {
                model.addAttribute("error", "이미 수강 중인 과목입니다.");
                return "student/enroll_form";
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "유효하지 않은 과목 코드입니다.");
            return "student/enroll_form";
        }
    }

    // 특정 과목의 과제 목록 확인
    @GetMapping("/course/{courseId}")
    public String viewCourseAssignments(@PathVariable Long courseId,
                                        @SessionAttribute("user") User student,
                                        Model model) {
        Course course = courseService.getCourseById(courseId);
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        return "student/course_assignments";
    }

    // 과제 제출 폼
    @GetMapping("/assignment/{assignmentId}/submit")
    public String submitForm(@PathVariable Long assignmentId,
                             @SessionAttribute("user") User student,
                             Model model) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        Optional<Submission> existingSubmission = submissionService.getSubmissionByAssignmentAndStudent(assignment, student);

        model.addAttribute("assignment", assignment);
        existingSubmission.ifPresent(submission -> model.addAttribute("existingSubmission", submission));
        return "student/submit_form";
    }

    // 과제 제출 처리
    @PostMapping("/assignment/{assignmentId}/submit")
    public String submitAssignment(@PathVariable Long assignmentId,
                                   @SessionAttribute("user") User student,
                                   @RequestParam("file") MultipartFile file) {
        // 실제 환경에서는 파일 저장 로직 구현 필요 (S3, local storage 등)
        String fileUrl = "/uploads/" + file.getOriginalFilename();
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        submissionService.submitAssignment(assignment, student, fileUrl);
        return "redirect:/student/course/" + assignment.getCourse().getCourseId();
    }

    // 내 제출물 및 점수 확인
    @GetMapping("/submissions")
    public String viewMySubmissions(@SessionAttribute("user") User student, Model model) {
        List<Submission> submissions = submissionService.getSubmissionsByStudent(student);
        model.addAttribute("submissions", submissions);
        return "student/my_submissions";
    }

    // 특정 과제의 내 제출물 상세 확인
    @GetMapping("/assignment/{assignmentId}/my-submission")
    public String viewMySubmission(@PathVariable Long assignmentId,
                                   @SessionAttribute("user") User student,
                                   Model model) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        Optional<Submission> submission = submissionService.getSubmissionByAssignmentAndStudent(assignment, student);

        model.addAttribute("assignment", assignment);
        submission.ifPresent(s -> model.addAttribute("submission", s));
        return "student/submission_detail";
    }
}