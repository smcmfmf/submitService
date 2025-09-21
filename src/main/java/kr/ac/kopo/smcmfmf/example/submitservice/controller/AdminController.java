package kr.ac.kopo.smcmfmf.example.submitservice.controller;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(@SessionAttribute("user") User admin, Model model) {
        if (!admin.isAdmin()) {
            log.warn("관리자가 아닌 사용자가 관리자 페이지 접근 시도: {}", admin.getEmail());
            return "redirect:/";
        }

        // 대시보드 통계 정보
        long pendingCount = userService.countByAccountStatus(User.AccountStatus.PENDING);
        long approvedCount = userService.countByAccountStatus(User.AccountStatus.APPROVED);
        long rejectedCount = userService.countByAccountStatus(User.AccountStatus.REJECTED);
        long totalUsers = userService.countAllUsers();

        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalUsers", totalUsers);

        // 최근 신청 목록 (5개)
        List<User> recentApplications = userService.findRecentApplications(5);
        model.addAttribute("recentApplications", recentApplications);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "ALL") String status,
                            @RequestParam(defaultValue = "ALL") String role,
                            @SessionAttribute("user") User admin,
                            Model model) {

        if (!admin.isAdmin()) {
            return "redirect:/";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userService.findUsersWithFilters(status, role, pageable);

        model.addAttribute("users", users);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentRole", role);
        model.addAttribute("statuses", User.AccountStatus.values());
        model.addAttribute("roles", User.Role.values());

        return "admin/user_list";
    }

    @GetMapping("/users/pending")
    public String pendingUsers(@SessionAttribute("user") User admin, Model model) {
        if (!admin.isAdmin()) {
            return "redirect:/";
        }

        List<User> pendingUsers = userService.findByAccountStatus(User.AccountStatus.PENDING);
        model.addAttribute("pendingUsers", pendingUsers);
        return "admin/pending_users";
    }

    @PostMapping("/users/{userId}/approve")
    public String approveUser(@PathVariable Long userId,
                              @RequestParam(required = false) String reason,
                              @SessionAttribute("user") User admin,
                              RedirectAttributes redirectAttributes) {
        try {
            if (!admin.isAdmin()) {
                throw new SecurityException("관리자 권한이 필요합니다.");
            }

            User user = userService.approveUser(userId, admin, reason);
            redirectAttributes.addFlashAttribute("success",
                    user.getName() + "(" + user.getEmail() + ") 계정을 승인했습니다.");

            log.info("계정 승인 완료: {} -> {} by {}", user.getEmail(), user.getRole(), admin.getEmail());

        } catch (Exception e) {
            log.error("계정 승인 중 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "계정 승인 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users/pending";
    }

    @PostMapping("/users/{userId}/reject")
    public String rejectUser(@PathVariable Long userId,
                             @RequestParam(required = false) String reason,
                             @SessionAttribute("user") User admin,
                             RedirectAttributes redirectAttributes) {
        try {
            if (!admin.isAdmin()) {
                throw new SecurityException("관리자 권한이 필요합니다.");
            }

            User user = userService.rejectUser(userId, admin, reason);
            redirectAttributes.addFlashAttribute("success",
                    user.getName() + "(" + user.getEmail() + ") 계정을 거부했습니다.");

            log.info("계정 거부 완료: {} by {}", user.getEmail(), admin.getEmail());

        } catch (Exception e) {
            log.error("계정 거부 중 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "계정 거부 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users/pending";
    }

    @GetMapping("/users/{userId}")
    public String viewUser(@PathVariable Long userId,
                           @SessionAttribute("user") User admin,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            if (!admin.isAdmin()) {
                return "redirect:/";
            }

            User user = userService.findById(userId);
            model.addAttribute("viewUser", user);
            return "admin/user_detail";

        } catch (RuntimeException e) {
            log.error("사용자 조회 중 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "사용자 정보를 찾을 수 없습니다.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            log.error("사용자 상세보기 중 예상치 못한 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "사용자 정보를 불러올 수 없습니다.");
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/{userId}/suspend")
    public String suspendUser(@PathVariable Long userId,
                              @RequestParam String reason,
                              @SessionAttribute("user") User admin,
                              RedirectAttributes redirectAttributes) {
        try {
            if (!admin.isAdmin()) {
                throw new SecurityException("관리자 권한이 필요합니다.");
            }

            User user = userService.suspendUser(userId, admin, reason);
            redirectAttributes.addFlashAttribute("success",
                    user.getName() + "(" + user.getEmail() + ") 계정을 정지했습니다.");

            log.info("계정 정지 완료: {} by {}", user.getEmail(), admin.getEmail());

        } catch (Exception e) {
            log.error("계정 정지 중 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "계정 정지 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/reactivate")
    public String reactivateUser(@PathVariable Long userId,
                                 @RequestParam String reason,
                                 @SessionAttribute("user") User admin,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!admin.isAdmin()) {
                throw new SecurityException("관리자 권한이 필요합니다.");
            }

            User user = userService.reactivateUser(userId, admin, reason);
            redirectAttributes.addFlashAttribute("success",
                    user.getName() + "(" + user.getEmail() + ") 계정을 재활성화했습니다.");

            log.info("계정 재활성화 완료: {} by {}", user.getEmail(), admin.getEmail());

        } catch (Exception e) {
            log.error("계정 재활성화 중 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "계정 재활성화 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }
}