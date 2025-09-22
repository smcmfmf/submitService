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

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;

    // 관리자 권한 확인 헬퍼 메서드
    private User getAdminFromSession(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) {
            return null;
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        log.debug("관리자 대시보드 접근 요청");

        User admin = getAdminFromSession(session);
        if (admin == null) {
            log.warn("권한 없는 사용자의 관리자 페이지 접근 시도");
            return "redirect:/";
        }

        log.info("관리자 대시보드 접근: {}", admin.getEmail());

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

        log.debug("대시보드 데이터 로드 완료: pending={}, approved={}, rejected={}, total={}",
                pendingCount, approvedCount, rejectedCount, totalUsers);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "ALL") String status,
                            @RequestParam(defaultValue = "ALL") String role,
                            HttpSession session,
                            Model model) {

        log.debug("사용자 목록 조회 요청: page={}, size={}, status={}, role={}", page, size, status, role);

        User admin = getAdminFromSession(session);
        if (admin == null) {
            log.warn("권한 없는 사용자의 사용자 목록 접근 시도");
            return "redirect:/";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userService.findUsersWithFilters(status, role, pageable);

        model.addAttribute("users", users);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentRole", role);
        model.addAttribute("statuses", User.AccountStatus.values());
        model.addAttribute("roles", User.Role.values());

        log.debug("사용자 목록 로드 완료: 총 {} 페이지 중 {} 페이지, {} 건",
                users.getTotalPages(), page + 1, users.getContent().size());

        return "admin/user_list";
    }

    @GetMapping("/users/pending")
    public String pendingUsers(HttpSession session, Model model) {
        log.debug("승인 대기 사용자 목록 조회 요청");

        User admin = getAdminFromSession(session);
        if (admin == null) {
            log.warn("권한 없는 사용자의 승인 대기 목록 접근 시도");
            return "redirect:/";
        }

        List<User> pendingUsers = userService.findByAccountStatus(User.AccountStatus.PENDING);
        model.addAttribute("pendingUsers", pendingUsers);

        log.debug("승인 대기 사용자 {} 명 조회 완료", pendingUsers.size());

        return "admin/pending_users";
    }

    // 거부된 사용자 목록 보기 (새로 추가)
    @GetMapping("/users/rejected")
    public String rejectedUsers(HttpSession session, Model model) {
        log.debug("거부된 사용자 목록 조회 요청");

        User admin = getAdminFromSession(session);
        if (admin == null) {
            log.warn("권한 없는 사용자의 거부된 사용자 목록 접근 시도");
            return "redirect:/";
        }

        List<User> rejectedUsers = userService.findByAccountStatus(User.AccountStatus.REJECTED);
        model.addAttribute("rejectedUsers", rejectedUsers);

        log.debug("거부된 사용자 {} 명 조회 완료", rejectedUsers.size());

        return "admin/rejected_users";
    }

    @PostMapping("/users/{userId}/approve")
    public String approveUser(@PathVariable Long userId,
                              @RequestParam(required = false) String reason,
                              HttpSession session,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {

        log.info("사용자 승인 요청: userId={}, reason={}", userId, reason);

        try {
            User admin = getAdminFromSession(session);
            if (admin == null) {
                log.error("권한 없는 사용자의 승인 시도");
                redirectAttributes.addFlashAttribute("error", "관리자 권한이 필요합니다.");
                return "redirect:/";
            }

            User user = userService.approveUser(userId, admin, reason);
            redirectAttributes.addFlashAttribute("success",
                    user.getName() + "(" + user.getEmail() + ") 계정을 승인했습니다.");

            log.info("계정 승인 완료: {} -> {} by {}", user.getEmail(), user.getRole(), admin.getEmail());

        } catch (Exception e) {
            log.error("계정 승인 중 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "계정 승인 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 거부된 사용자 페이지에서 온 경우 거부된 사용자 페이지로 리다이렉트
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/rejected")) {
            return "redirect:/admin/users/rejected";
        }

        return "redirect:/admin/users/pending";
    }

    @PostMapping("/users/{userId}/reject")
    public String rejectUser(@PathVariable Long userId,
                             @RequestParam(required = false) String reason,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        log.info("사용자 거부 요청: userId={}, reason={}", userId, reason);

        try {
            User admin = getAdminFromSession(session);
            if (admin == null) {
                log.error("권한 없는 사용자의 거부 시도");
                redirectAttributes.addFlashAttribute("error", "관리자 권한이 필요합니다.");
                return "redirect:/";
            }

            User user = userService.rejectUser(userId, admin, reason);
            redirectAttributes.addFlashAttribute("success",
                    user.getName() + "(" + user.getEmail() + ") 계정을 거부했습니다. 나중에 다시 승인할 수 있습니다.");

            log.info("계정 거부 완료: {} by {}", user.getEmail(), admin.getEmail());

        } catch (Exception e) {
            log.error("계정 거부 중 오류 발생: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "계정 거부 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users/pending";
    }

    @GetMapping("/users/{userId}")
    public String viewUser(@PathVariable Long userId,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        log.debug("사용자 상세 조회 요청: userId={}", userId);

        try {
            User admin = getAdminFromSession(session);
            if (admin == null) {
                log.warn("권한 없는 사용자의 사용자 상세 조회 시도");
                return "redirect:/";
            }

            User user = userService.findById(userId);
            model.addAttribute("viewUser", user);

            log.debug("사용자 상세 조회 완료: {}", user.getEmail());

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
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        log.info("사용자 정지 요청: userId={}, reason={}", userId, reason);

        try {
            User admin = getAdminFromSession(session);
            if (admin == null) {
                log.error("권한 없는 사용자의 정지 시도");
                redirectAttributes.addFlashAttribute("error", "관리자 권한이 필요합니다.");
                return "redirect:/";
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
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        log.info("사용자 재활성화 요청: userId={}, reason={}", userId, reason);

        try {
            User admin = getAdminFromSession(session);
            if (admin == null) {
                log.error("권한 없는 사용자의 재활성화 시도");
                redirectAttributes.addFlashAttribute("error", "관리자 권한이 필요합니다.");
                return "redirect:/";
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