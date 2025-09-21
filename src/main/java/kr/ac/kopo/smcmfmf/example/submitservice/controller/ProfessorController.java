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
        } catch (IllegalArgumentException e) {
            log.warn("과제 생성 실패 - 유효성 검사 오류: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assignment", assignment);
            model.addAttribute("courseId", courseId);
            return "professor/assignment_form";
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
    public String editAssignmentForm(@PathVariable Long assignmentId, Model model) {
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
                                   @SessionAttribute("user") User professor,
                                   Model model) {
        try {
            Assignment existingAssignment = assignmentService.getAssignmentById(assignmentId);

            // 기존 과제 정보 업데이트
            existingAssignment.setTitle(assignmentForm.getTitle());
            existingAssignment.setDescription(assignmentForm.getDescription());
            existingAssignment.setDeadline(assignmentForm.getDeadline());

            // 새 첨부파일이 있는 경우 저장
            if (attachmentFile != null && !attachmentFile.isEmpty()) {
                String savedFileName = fileService.saveAssignmentAttachment(
                        attachmentFile,
                        professor.getName(),
                        existingAssignment.getTitle()
                );
                String fileUrl = "/files/download/" + savedFileName;
                existingAssignment.setAttachmentUrl(fileUrl);
                log.info("과제 첨부파일 업데이트 완료: {}", savedFileName);
            }

            assignmentService.updateAssignment(existingAssignment);
            return "redirect:/professor/course/" + existingAssignment.getCourse().getCourseId();
        } catch (IllegalArgumentException e) {
            log.warn("과제 수정 실패 - 유효성 검사 오류: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assignment", assignmentForm);
            model.addAttribute("courseId", assignmentForm.getCourse().getCourseId());
            return "professor/assignment_edit_form";
        } catch (Exception e) {
            log.error("과제 수정 중 오류 발생", e);
            model.addAttribute("error", "과제 수정 중 오류가 발생했습니다.");
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            model.addAttribute("assignment", assignment);
            model.addAttribute("courseId", assignment.getCourse().getCourseId());
            return "professor/assignment_edit_form";
        }
    }

    // 과제 마감일 연장 폼
    @GetMapping("/assignment/{assignmentId}/extend")
    public String extendDeadlineForm(@PathVariable Long assignmentId, Model model) {
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        model.addAttribute("assignment", assignment);
        return "professor/extend_deadline_form";
    }

    // 과제 마감일 연장 처리
    @PostMapping("/assignment/{assignmentId}/extend")
    public String extendDeadline(@PathVariable Long assignmentId,
                                 @RequestParam("newDeadline") LocalDateTime newDeadline,
                                 RedirectAttributes redirectAttributes) {
        try {
            Assignment assignment = assignmentService.extendDeadline(assignmentId, newDeadline);
            redirectAttributes.addFlashAttribute("success",
                    "과제 마감일이 성공적으로 연장되었습니다: " +
                            newDeadline.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")));
            return "redirect:/professor/course/" + assignment.getCourse().getCourseId();
        } catch (IllegalArgumentException e) {
            log.warn("마감일 연장 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professor/assignment/" + assignmentId + "/extend";
        } catch (Exception e) {
            log.error("마감일 연장 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "마감일 연장 중 오류가 발생했습니다.");
            return "redirect:/professor/assignment/" + assignmentId + "/extend";
        }
    }

    // 특정 과제의 제출물 확인 메소드 수정
    @GetMapping("/assignment/{assignmentId}/submissions")
    public String viewSubmissions(@PathVariable Long assignmentId, Model model) {
        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId);
            List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignment);

            // 상세한 통계 정보 계산
            long totalSubmissions = submissions.size();

            // 평가 완료된 제출물 (isGraded = true)
            long gradedCount = submissions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsGraded()))
                    .count();

            // 임시 점수가 있지만 평가 완료되지 않은 제출물 (grade != null && isGraded = false)
            long tempGradedCount = submissions.stream()
                    .filter(s -> s.getGrade() != null && !Boolean.TRUE.equals(s.getIsGraded()))
                    .count();

            // 아직 점수가 없는 제출물 (grade == null)
            long pendingCount = submissions.stream()
                    .filter(s -> s.getGrade() == null)
                    .count();

            // 평가 대기 중 (평가 완료되지 않은 모든 제출물)
            long ungradedCount = totalSubmissions - gradedCount;

            // 평균 점수 계산 (평가 완료된 것만)
            double averageScore = submissions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsGraded()) && s.getGrade() != null)
                    .mapToDouble(s -> s.getGrade().doubleValue())
                    .average()
                    .orElse(0.0);

            model.addAttribute("assignment", assignment);
            model.addAttribute("submissions", submissions);
            model.addAttribute("totalSubmissions", totalSubmissions);
            model.addAttribute("gradedCount", gradedCount);
            model.addAttribute("tempGradedCount", tempGradedCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("ungradedCount", ungradedCount);
            model.addAttribute("averageScore", String.format("%.1f", averageScore));

            log.info("제출물 조회 완료: assignment={}, total={}, graded={}, temp={}, pending={}",
                    assignment.getTitle(), totalSubmissions, gradedCount, tempGradedCount, pendingCount);

            return "professor/submissions";

        } catch (Exception e) {
            log.error("제출물 목록 조회 중 오류 발생: assignmentId={}", assignmentId, e);
            model.addAttribute("error", "제출물 목록을 불러올 수 없습니다.");
            return "professor/dashboard";
        }
    }

    // 점수 및 피드백 입력 폼
    @GetMapping("/submission/{submissionId}/grade")
    public String gradeForm(@PathVariable Long submissionId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Submission submission = submissionService.getSubmissionById(submissionId);

            // 평가 완료된 과제인지 확인
            if (submission.getIsGraded()) {
                redirectAttributes.addFlashAttribute("error", "이미 평가가 완료된 과제입니다. 수정할 수 없습니다.");
                return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
            }

            model.addAttribute("submission", submission);
            return "professor/grade_form";
        } catch (Exception e) {
            log.error("채점 폼 로드 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "채점 폼을 불러올 수 없습니다.");
            return "redirect:/professor/dashboard";
        }
    }

    // 임시 점수 저장 (평가 미완료)
    @PostMapping("/submission/{submissionId}/grade")
    public String saveGrade(@PathVariable Long submissionId,
                            @RequestParam BigDecimal grade,
                            @RequestParam String feedback,
                            RedirectAttributes redirectAttributes) {
        try {
            Submission submission = submissionService.updateGradeAndFeedback(submissionId, grade, feedback);
            redirectAttributes.addFlashAttribute("success", "점수가 임시 저장되었습니다. 평가를 완료하려면 '평가 완료' 버튼을 클릭하세요.");
            return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
        } catch (IllegalStateException e) {
            log.warn("점수 저장 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professor/submission/" + submissionId + "/grade";
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
                                  RedirectAttributes redirectAttributes) {
        try {
            Submission submission = submissionService.completeGrading(submissionId, grade, feedback);
            redirectAttributes.addFlashAttribute("success",
                    "평가가 완료되었습니다. 이제 학생과 교수 모두 수정할 수 없습니다.");
            return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
        } catch (IllegalStateException e) {
            log.warn("평가 완료 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professor/submission/" + submissionId + "/grade";
        } catch (Exception e) {
            log.error("평가 완료 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "평가 완료 중 오류가 발생했습니다.");
            return "redirect:/professor/submission/" + submissionId + "/grade";
        }
    }

    // 평가 완료 취소 (관리자 기능)
    @PostMapping("/submission/{submissionId}/cancel-completion")
    public String cancelGradingCompletion(@PathVariable Long submissionId,
                                          @SessionAttribute("user") User professor,
                                          RedirectAttributes redirectAttributes) {
        try {
            // 관리자 권한 확인 (필요시 구현)
            Submission submission = submissionService.cancelGradingCompletion(submissionId);
            redirectAttributes.addFlashAttribute("success", "평가 완료가 취소되었습니다. 이제 다시 수정할 수 있습니다.");
            return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
        } catch (IllegalStateException e) {
            log.warn("평가 완료 취소 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professor/dashboard";
        } catch (Exception e) {
            log.error("평가 완료 취소 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "평가 완료 취소 중 오류가 발생했습니다.");
            return "redirect:/professor/dashboard";
        }
    }

    // 기존 메소드 (하위 호환성을 위해 유지)
    @Deprecated
    @PostMapping("/submission/{submissionId}/grade-legacy")
    public String gradeSubmissionLegacy(@PathVariable Long submissionId,
                                        @RequestParam BigDecimal grade,
                                        @RequestParam String feedback) {
        Submission submission = submissionService.updateGradeAndFeedback(submissionId, grade, feedback);
        return "redirect:/professor/assignment/" + submission.getAssignment().getAssignmentId() + "/submissions";
    }
}
