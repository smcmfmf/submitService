package kr.ac.kopo.smcmfmf.example.submitservice.controller;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import kr.ac.kopo.smcmfmf.example.submitservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping("/")
    public String home(Model model) {
        // 첫 번째 관리자 계정이 없는 경우 생성 페이지로 이동
        if (!userService.hasAdminAccount()) {
            return "redirect:/setup-admin";
        }
        return "login";
    }

    @GetMapping("/setup-admin")
    public String setupAdminForm(Model model) {
        // 이미 관리자가 있다면 로그인 페이지로
        if (userService.hasAdminAccount()) {
            return "redirect:/";
        }
        model.addAttribute("user", new User());
        return "setup_admin";
    }

    @PostMapping("/setup-admin")
    public String setupAdmin(@ModelAttribute User user, Model model) {
        try {
            if (userService.hasAdminAccount()) {
                model.addAttribute("error", "이미 관리자 계정이 존재합니다.");
                return "setup_admin";
            }

            userService.createFirstAdminAccount(user.getName(), user.getEmail(), user.getPassword());
            log.info("첫 번째 관리자 계정 생성 완료: {}", user.getEmail());
            return "redirect:/?admin_created=true";
        } catch (Exception e) {
            log.error("관리자 계정 생성 중 오류 발생: ", e);
            model.addAttribute("error", "관리자 계정 생성 중 오류가 발생했습니다.");
            return "setup_admin";
        }
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@ModelAttribute User user, Model model) {
        try {
            // 이메일 중복 확인
            Optional<User> existingUser = userService.findByEmail(user.getEmail());
            if (existingUser.isPresent()) {
                model.addAttribute("error", "이미 존재하는 이메일입니다.");
                return "signup";
            }

            // 실제 환경에서는 비밀번호 암호화 필요
            userService.registerUser(user);

            if (user.getRole() == User.Role.ADMIN) {
                log.info("새 관리자 등록 완료: {} ({})", user.getName(), user.getEmail());
                return "redirect:/?signup=admin_success";
            } else {
                log.info("새 사용자 등록 완료 (승인 대기): {} ({})", user.getName(), user.getEmail());
                return "redirect:/?signup=pending";
            }
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: ", e);
            model.addAttribute("error", "회원가입 중 오류가 발생했습니다.");
            return "signup";
        }
    }

    // 로그인 페이지 (GET) - 메시지 처리
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "signup", required = false) String signup,
                            @RequestParam(value = "admin_created", required = false) String adminCreated,
                            Model model) {
        if ("unauthorized".equals(error)) {
            model.addAttribute("error", "로그인이 필요합니다.");
        }

        if ("pending".equals(signup)) {
            model.addAttribute("success", "회원가입이 완료되었습니다. 관리자 승인을 기다려주세요.");
        } else if ("admin_success".equals(signup)) {
            model.addAttribute("success", "관리자 계정이 생성되었습니다. 로그인해주세요.");
        }

        if ("true".equals(adminCreated)) {
            model.addAttribute("success", "첫 번째 관리자 계정이 생성되었습니다. 로그인해주세요.");
        }

        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            log.info("로그인 시도: {}", email);

            Optional<User> optionalUser = userService.findByEmail(email);

            if (optionalUser.isEmpty()) {
                log.warn("존재하지 않는 사용자: {}", email);
                model.addAttribute("error", "존재하지 않는 사용자입니다.");
                return "login";
            }

            User user = optionalUser.get();

            if (!user.getPassword().equals(password)) {
                log.warn("잘못된 비밀번호: {}", email);
                model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
                return "login";
            }

            // 계정 승인 상태 확인
            if (!userService.canUserLogin(user)) {
                String message = getAccountStatusMessage(user.getAccountStatus());
                log.warn("승인되지 않은 계정 로그인 시도: {} (상태: {})", email, user.getAccountStatus());
                model.addAttribute("error", message);
                return "login";
            }

            // 세션에 사용자 정보 저장
            session.setAttribute("user", user);
            log.info("로그인 성공: {} ({})", user.getName(), user.getRole());

            // 역할에 따른 리다이렉트
            if (user.getRole() == User.Role.ADMIN) {
                return "redirect:/admin/dashboard";
            } else if (user.getRole() == User.Role.PROFESSOR) {
                return "redirect:/professor/dashboard";
            } else if (user.getRole() == User.Role.STUDENT) {
                return "redirect:/student/dashboard";
            } else {
                return "redirect:/";
            }

        } catch (Exception e) {
            log.error("로그인 중 오류 발생: ", e);
            model.addAttribute("error", "로그인 중 오류가 발생했습니다.");
            return "login";
        }
    }

    private String getAccountStatusMessage(User.AccountStatus status) {
        return switch (status) {
            case PENDING -> "계정 승인 대기 중입니다. 관리자의 승인을 기다려주세요.";
            case REJECTED -> "계정이 거부되었습니다. 자세한 내용은 관리자에게 문의하세요.";
            case SUSPENDED -> "계정이 정지되었습니다. 자세한 내용은 관리자에게 문의하세요.";
            default -> "계정에 문제가 있습니다. 관리자에게 문의하세요.";
        };
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        log.info("로그아웃 완료");
        return "redirect:/";
    }

    @GetMapping("/account-status")
    public String checkAccountStatus(@RequestParam String email, Model model) {
        try {
            Optional<User> optionalUser = userService.findByEmail(email);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                model.addAttribute("user", user);
                model.addAttribute("statusMessage", getAccountStatusMessage(user.getAccountStatus()));
            } else {
                model.addAttribute("error", "존재하지 않는 이메일입니다.");
            }
        } catch (Exception e) {
            log.error("계정 상태 확인 중 오류 발생: ", e);
            model.addAttribute("error", "계정 상태 확인 중 오류가 발생했습니다.");
        }
        return "account_status";
    }
}
