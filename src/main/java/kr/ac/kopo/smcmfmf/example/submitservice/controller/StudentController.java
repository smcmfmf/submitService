package kr.ac.kopo.smcmfmf.example.submitservice.controller;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Submission;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.service.AssignmentService;
import kr.ac.kopo.smcmfmf.example.submitservice.service.CourseService;
import kr.ac.kopo.smcmfmf.example.submitservice.service.FileService;
import kr.ac.kopo.smcmfmf.example.submitservice.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
public class StudentController {
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final FileService fileService;

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
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            Optional<Submission> existingSubmission = submissionService.getSubmissionByAssignmentAndStudent(assignment, student);

            // 기존 제출물이 있고 평가가 완료된 경우 재제출 불가
            if (existingSubmission.isPresent() && existingSubmission.get().getIsGraded()) {
                redirectAttributes.addFlashAttribute("error",
                        "이미 평가가 완료된 과제입니다. 재제출할 수 없습니다.");
                return "redirect:/student/assignment/" + assignmentId + "/my-submission";
            }

            model.addAttribute("assignment", assignment);
            existingSubmission.ifPresent(submission -> model.addAttribute("existingSubmission", submission));

            // 재제출 가능 여부 추가
            boolean canResubmit = submissionService.canResubmit(assignment, student);
            model.addAttribute("canResubmit", canResubmit);

            return "student/submit_form";
        } catch (Exception e) {
            log.error("제출 폼 로드 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "제출 폼을 불러올 수 없습니다.");
            return "redirect:/student/dashboard";
        }
    }

    // 과제 제출 처리
    @PostMapping("/assignment/{assignmentId}/submit")
    public String submitAssignment(@PathVariable Long assignmentId,
                                   @SessionAttribute("user") User student,
                                   @RequestParam("file") MultipartFile file,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "파일을 선택해주세요.");
                Assignment assignment = assignmentService.getAssignmentById(assignmentId);
                model.addAttribute("assignment", assignment);
                return "student/submit_form";
            }

            Assignment assignment = assignmentService.getAssignmentById(assignmentId);

            // 재제출 가능 여부 확인
            if (!submissionService.canResubmit(assignment, student)) {
                redirectAttributes.addFlashAttribute("error",
                        "이미 평가가 완료된 과제입니다. 재제출할 수 없습니다.");
                return "redirect:/student/assignment/" + assignmentId + "/my-submission";
            }

            // 파일 실제 저장
            String savedFileName = fileService.saveStudentSubmission(file, student.getName());
            String fileUrl = "/files/download/" + savedFileName;

            submissionService.submitAssignment(assignment, student, fileUrl);

            redirectAttributes.addFlashAttribute("success", "과제가 성공적으로 제출되었습니다!");
            return "redirect:/student/course/" + assignment.getCourse().getCourseId();

        } catch (IllegalStateException e) {
            log.warn("파일 업로드 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            model.addAttribute("assignment", assignment);
            return "student/submit_form";
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생", e);
            model.addAttribute("error", "파일 업로드 중 오류가 발생했습니다.");
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            model.addAttribute("assignment", assignment);
            return "student/submit_form";
        }
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
        submission.ifPresent(s -> {
            model.addAttribute("submission", s);
            // 재제출 가능 여부 추가
            model.addAttribute("canResubmit", !s.getIsGraded());
        });

        // 재제출 가능 여부 (제출물이 없는 경우)
        if (submission.isEmpty()) {
            model.addAttribute("canResubmit", true);
        }

        return "student/submission_detail";
    }
}