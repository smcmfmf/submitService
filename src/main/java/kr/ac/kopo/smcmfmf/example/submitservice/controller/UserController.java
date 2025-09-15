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
    public String home() {
        return "login";
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
            log.info("새 사용자 등록 완료: {} ({})", user.getName(), user.getEmail());
            return "redirect:/?signup=success";
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: ", e);
            model.addAttribute("error", "회원가입 중 오류가 발생했습니다.");
            return "signup";
        }
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

            // 세션에 사용자 정보 저장
            session.setAttribute("user", user);
            log.info("로그인 성공: {} ({})", user.getName(), user.getRole());

            // 역할에 따른 리다이렉트
            if (user.getRole() == User.Role.PROFESSOR) {
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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        log.info("로그아웃 완료");
        return "redirect:/";
    }
}