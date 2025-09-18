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

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/professor")
@RequiredArgsConstructor
@Slf4j
public class ProfessorController {
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final FileService fileService;

    @GetMapping("/dashboard")
    public String dashboard(@SessionAttribute("user") User user, Model model) {
        List<Course> courses = courseService.getProfessorCourses(user);
        model.addAttribute("courses", courses);
        return "professor/dashboard";
    }

    // 과목 생성 폼
    @GetMapping("/course/new")
    public String createCourseForm(Model model) {
        model.addAttribute("course", new Course());
        return "professor/course_form";
    }

    // 과목 생성
    @PostMapping("/course/new")
    public String createCourse(@ModelAttribute Course course,
                               @SessionAttribute("user") User user) {
        course.setProfessor(user);
        courseService.createCourse(course);
        return "redirect:/professor/dashboard";
    }

    // 특정 과목의 과제 목록 확인
    @GetMapping("/course/{courseId}")
    public String viewCourse(@PathVariable Long courseId, Model model) {
        Course course = courseService.getCourseById(courseId);
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        return "professor/course_detail";
    }

    // 과제 생성 폼
    @GetMapping("/course/{courseId}/assignment/new")
    public String createAssignmentForm(@PathVariable Long courseId, Model model) {
        model.addAttribute("assignment", new Assignment());
        model.addAttribute("courseId", courseId);
        return "professor/assignment_form";
    }

    // 과제 생성 (파일 업로드 지원)
    @PostMapping("/course/{courseId}/assignment/new")
    public String createAssignment(@PathVariable Long courseId,
                                   @ModelAttribute Assignment assignment,
                                   @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
                                   @SessionAttribute("user") User professor,
                                   Model model) {
        try {
            Course course = courseService.getCourseById(courseId);
            assignment.setCourse(course);

            // 첨부파일이 있는 경우 저장
            if (attachmentFile != null && !attachmentFile.isEmpty()) {
                String savedFileName = fileService.saveAssignmentAttachment(
                        attachmentFile,
                        professor.getName(),
                        assignment.getTitle()
                );
                String fileUrl = "/files/download/" + savedFileName;
                assignment.setAttachmentUrl(fileUrl);
                log.info("과제 첨부파일 저장 완료: {}", savedFileName);
            }

            assignmentService.createAssignment(assignment);
            return "redirect:/professor/course/" + courseId;
        } catch (Exception e) {
            log.error("과제 생성 중 오류 발생", e);
            model.addAttribute("error", "과제 생성 중 오류가 발생했습니다.");
            model.addAttribute("assignment", assignment);
            model.addAttribute("courseId", courseId);
            return "professor/assignment_form";
        }
    }

    // 특정 과제의 제출물 확인
    @GetMapping("/assignment/{assignmentId}/submissions")
    public String viewSubmissions(@PathVariable Long assignmentId, Model model) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        return "professor/submissions";
    }

    // 점수 및 피드백 입력 폼
    @GetMapping("/submission/{submissionId}/grade")
    public String gradeForm(@PathVariable Long submissionId, Model model) {
        Submission submission = submissionService.getSubmissionById(submissionId);
        model.addAttribute("submission", submission);
        return "professor/grade_form";
    }

    // 점수 및 피드백 저장
    @PostMapping("/submission/{submissionId}/grade")
    public String gradeSubmission(@PathVariable Long submissionId,
                                  @RequestParam BigDecimal grade,
                                  @RequestParam String feedback) {
        Submission submission = submissionService.updateGradeAndFeedback(submissionId, grade, feedback);
        return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
    }
}