package kr.ac.kopo.smcmfmf.example.submitservice.controller;

import jakarta.servlet.http.HttpSession;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // 교수 권한 체크 헬퍼 메서드
    private String checkProfessorAuthAndRedirect(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.PROFESSOR) {
            log.warn("권한 없는 사용자의 교수 페이지 접근 시도");
            return "redirect:/login?error=unauthorized";
        }
        return null; // 권한이 있으면 null 반환
    }

    // 대시보드
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User user = (User) session.getAttribute("user");
        List<Course> courses = courseService.getProfessorCourses(user);
        model.addAttribute("courses", courses);
        return "professor/dashboard";
    }

    // 과목 생성 폼
    @GetMapping("/course/new")
    public String createCourseForm(HttpSession session, Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        model.addAttribute("course", new Course());
        return "professor/course_form";
    }

    // 과목 생성
    @PostMapping("/course/new")
    public String createCourse(@ModelAttribute Course course, HttpSession session) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User user = (User) session.getAttribute("user");
        course.setProfessor(user);
        courseService.createCourse(course);
        return "redirect:/professor/dashboard";
    }

    // 특정 과목의 과제 목록 확인
    @GetMapping("/course/{courseId}")
    public String viewCourse(@PathVariable Long courseId, HttpSession session, Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        Course course = courseService.getCourseById(courseId);
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        return "professor/course_detail";
    }

    // 과목 삭제 확인 페이지
    @GetMapping("/course/{courseId}/delete-confirm")
    public String confirmDeleteCourse(@PathVariable Long courseId,
                                      HttpSession session,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User professor = (User) session.getAttribute("user");
        log.info("과목 삭제 확인 페이지 요청: courseId={}, professor={}", courseId, professor.getName());

        try {
            Course course = courseService.getCourseById(courseId);
            if (!course.getProfessor().getId().equals(professor.getId())) {
                redirectAttributes.addFlashAttribute("error", "해당 과목을 삭제할 권한이 없습니다.");
                return "redirect:/professor/dashboard";
            }

            CourseService.CourseDeleteInfo deleteInfo = courseService.getCourseDeleteInfo(courseId);
            model.addAttribute("course", course);
            model.addAttribute("deleteInfo", deleteInfo);
            return "professor/course_delete";
        } catch (Exception e) {
            log.error("과목 삭제 확인 페이지 로드 중 오류: courseId={}", courseId, e);
            redirectAttributes.addFlashAttribute("error", "과목 정보를 불러올 수 없습니다: " + e.getMessage());
            return "redirect:/professor/dashboard";
        }
    }

    // 과목 삭제 실행
    @PostMapping("/course/{courseId}/delete")
    public String deleteCourse(@PathVariable Long courseId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User professor = (User) session.getAttribute("user");
        try {
            Course course = courseService.getCourseById(courseId);
            String courseName = course.getName();
            courseService.deleteCourse(courseId, professor);
            redirectAttributes.addFlashAttribute("success", "과목 '" + courseName + "'이(가) 성공적으로 삭제되었습니다.");
            return "redirect:/professor/dashboard";
        } catch (Exception e) {
            log.error("과목 삭제 중 오류 발생: courseId={}", courseId, e);
            redirectAttributes.addFlashAttribute("error", "과목 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/professor/dashboard";
        }
    }

    // 과제 생성 폼
    @GetMapping("/course/{courseId}/assignment/new")
    public String createAssignmentForm(@PathVariable Long courseId, HttpSession session, Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        model.addAttribute("assignment", new Assignment());
        model.addAttribute("courseId", courseId);
        return "professor/assignment_form";
    }

    // 과제 생성
    @PostMapping("/course/{courseId}/assignment/new")
    public String createAssignment(@PathVariable Long courseId,
                                   @ModelAttribute Assignment assignment,
                                   @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
                                   HttpSession session,
                                   Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User professor = (User) session.getAttribute("user");
        try {
            Course course = courseService.getCourseById(courseId);
            assignment.setCourse(course);

            if (attachmentFile != null && !attachmentFile.isEmpty()) {
                String savedFileName = fileService.saveAssignmentAttachment(
                        attachmentFile,
                        professor.getName(),
                        assignment.getTitle()
                );
                assignment.setAttachmentUrl("/files/download/" + savedFileName);
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

    // 과제 수정 폼
    @GetMapping("/assignment/{assignmentId}/edit")
    public String editAssignmentForm(@PathVariable Long assignmentId, HttpSession session, Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        model.addAttribute("assignment", assignment);
        model.addAttribute("courseId", assignment.getCourse().getCourseId());
        return "professor/assignment_edit_form";
    }

    // 과제 수정 처리
    @PostMapping("/assignment/{assignmentId}/edit")
    public String updateAssignment(@PathVariable Long assignmentId,
                                   @ModelAttribute Assignment assignmentForm,
                                   @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
                                   HttpSession session,
                                   Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User professor = (User) session.getAttribute("user");
        try {
            Assignment existingAssignment = assignmentService.getAssignmentById(assignmentId);
            existingAssignment.setTitle(assignmentForm.getTitle());
            existingAssignment.setDescription(assignmentForm.getDescription());
            existingAssignment.setDeadline(assignmentForm.getDeadline());

            if (attachmentFile != null && !attachmentFile.isEmpty()) {
                String savedFileName = fileService.saveAssignmentAttachment(
                        attachmentFile,
                        professor.getName(),
                        existingAssignment.getTitle()
                );
                existingAssignment.setAttachmentUrl("/files/download/" + savedFileName);
            }

            assignmentService.updateAssignment(existingAssignment);
            return "redirect:/professor/course/" + existingAssignment.getCourse().getCourseId();
        } catch (Exception e) {
            log.error("과제 수정 중 오류 발생", e);
            model.addAttribute("error", "과제 수정 중 오류가 발생했습니다.");
            model.addAttribute("assignment", assignmentForm);
            return "professor/assignment_edit_form";
        }
    }

    // 과제 삭제 확인
    @GetMapping("/assignment/{assignmentId}/delete-confirm")
    public String confirmDeleteAssignment(@PathVariable Long assignmentId,
                                          HttpSession session,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User professor = (User) session.getAttribute("user");
        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            if (!assignment.getCourse().getProfessor().getId().equals(professor.getId())) {
                redirectAttributes.addFlashAttribute("error", "해당 과제를 삭제할 권한이 없습니다.");
                return "redirect:/professor/dashboard";
            }
            List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);
            model.addAttribute("assignment", assignment);
            model.addAttribute("submissionCount", submissions.size());
            return "professor/assignment_delete";
        } catch (Exception e) {
            log.error("과제 삭제 확인 페이지 로드 중 오류", e);
            redirectAttributes.addFlashAttribute("error", "과제 정보를 불러올 수 없습니다.");
            return "redirect:/professor/dashboard";
        }
    }

    // 과제 삭제 실행
    @PostMapping("/assignment/{assignmentId}/delete")
    public String deleteAssignment(@PathVariable Long assignmentId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        User professor = (User) session.getAttribute("user");
        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            Long courseId = assignment.getCourse().getCourseId();
            assignmentService.deleteAssignment(assignmentId);
            redirectAttributes.addFlashAttribute("success", "과제 '" + assignment.getTitle() + "'이 삭제되었습니다.");
            return "redirect:/professor/course/" + courseId;
        } catch (Exception e) {
            log.error("과제 삭제 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "과제 삭제 중 오류가 발생했습니다.");
            return "redirect:/professor/dashboard";
        }
    }

    // 과제 마감일 연장 폼
    @GetMapping("/assignment/{assignmentId}/extend")
    public String extendDeadlineForm(@PathVariable Long assignmentId, HttpSession session, Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        model.addAttribute("assignment", assignment);
        return "professor/extend_deadline_form";
    }

    // 과제 마감일 연장 처리
    @PostMapping("/assignment/{assignmentId}/extend")
    public String extendDeadline(@PathVariable Long assignmentId,
                                 @RequestParam("newDeadline") LocalDateTime newDeadline,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        try {
            Assignment assignment = assignmentService.extendDeadline(assignmentId, newDeadline);
            redirectAttributes.addFlashAttribute("success",
                    "과제 마감일이 연장되었습니다: " +
                            newDeadline.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")));
            return "redirect:/professor/course/" + assignment.getCourse().getCourseId();
        } catch (Exception e) {
            log.error("마감일 연장 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "마감일 연장 중 오류가 발생했습니다.");
            return "redirect:/professor/assignment/" + assignmentId + "/extend";
        }
    }

    // 특정 과제 제출물 확인
    @GetMapping("/assignment/{assignmentId}/submissions")
    public String viewSubmissions(@PathVariable Long assignmentId, HttpSession session, Model model) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);

            long totalSubmissions = submissions.size();
            long gradedCount = submissions.stream().filter(s -> Boolean.TRUE.equals(s.getIsGraded())).count();
            long tempGradedCount = submissions.stream()
                    .filter(s -> s.getGrade() != null && !Boolean.TRUE.equals(s.getIsGraded()))
                    .count();
            long pendingCount = submissions.stream().filter(s -> s.getGrade() == null).count();
            long ungradedCount = totalSubmissions - gradedCount;
            double averageScore = submissions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsGraded()) && s.getGrade() != null)
                    .mapToDouble(s -> s.getGrade().doubleValue())
                    .average().orElse(0.0);

            model.addAttribute("assignment", assignment);
            model.addAttribute("submissions", submissions);
            model.addAttribute("totalSubmissions", totalSubmissions);
            model.addAttribute("gradedCount", gradedCount);
            model.addAttribute("tempGradedCount", tempGradedCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("ungradedCount", ungradedCount);
            model.addAttribute("averageScore", String.format("%.1f", averageScore));

            return "professor/submissions";
        } catch (Exception e) {
            log.error("제출물 목록 조회 오류", e);
            model.addAttribute("error", "제출물 목록을 불러올 수 없습니다.");
            return "professor/dashboard";
        }
    }

    // 점수 입력 폼
    @GetMapping("/submission/{submissionId}/grade")
    public String gradeForm(@PathVariable Long submissionId, HttpSession session,
                            Model model, RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        try {
            Submission submission = submissionService.getSubmissionById(submissionId);
            if (submission.getIsGraded()) {
                redirectAttributes.addFlashAttribute("error", "이미 평가가 완료된 과제입니다.");
                return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
            }
            model.addAttribute("submission", submission);
            return "professor/grade_form";
        } catch (Exception e) {
            log.error("채점 폼 로드 중 오류", e);
            redirectAttributes.addFlashAttribute("error", "채점 폼을 불러올 수 없습니다.");
            return "redirect:/professor/dashboard";
        }
    }

    // 임시 점수 저장
    @PostMapping("/submission/{submissionId}/grade")
    public String saveGrade(@PathVariable Long submissionId,
                            @RequestParam BigDecimal grade,
                            @RequestParam String feedback,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        try {
            Submission submission = submissionService.updateGradeAndFeedback(submissionId, grade, feedback);
            redirectAttributes.addFlashAttribute("success", "점수가 임시 저장되었습니다.");
            return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
        } catch (Exception e) {
            log.error("점수 저장 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "점수 저장 중 오류가 발생했습니다.");
            return "redirect:/professor/submission/" + submissionId + "/grade";
        }
    }

    // 평가 완료 처리
    @PostMapping("/submission/{submissionId}/complete")
    public String completeGrading(@PathVariable Long submissionId,
                                  @RequestParam BigDecimal grade,
                                  @RequestParam String feedback,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        try {
            Submission submission = submissionService.completeGrading(submissionId, grade, feedback);
            redirectAttributes.addFlashAttribute("success", "평가가 완료되었습니다.");
            return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
        } catch (Exception e) {
            log.error("평가 완료 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "평가 완료 중 오류가 발생했습니다.");
            return "redirect:/professor/submission/" + submissionId + "/grade";
        }
    }

    // 평가 완료 취소 (관리자/교수 기능)
    @PostMapping("/submission/{submissionId}/cancel-completion")
    public String cancelGradingCompletion(@PathVariable Long submissionId,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        String redirect = checkProfessorAuthAndRedirect(session);
        if (redirect != null) return redirect;

        try {
            Submission submission = submissionService.cancelGradingCompletion(submissionId);
            redirectAttributes.addFlashAttribute("success", "평가 완료가 취소되었습니다.");
            return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
        } catch (Exception e) {
            log.error("평가 완료 취소 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "평가 완료 취소 중 오류가 발생했습니다.");
            return "redirect:/professor/dashboard";
        }
    }
}
