package kr.ac.kopo.smcmfmf.example.submitservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e, Model model, RedirectAttributes redirectAttributes) {
        log.error("예상치 못한 오류 발생: ", e);

        // Ajax 요청이 아닌 경우에만 에러 페이지로 이동
        redirectAttributes.addFlashAttribute("error", "시스템 오류가 발생했습니다: " + e.getMessage());
        return "redirect:/admin/dashboard";
    }

    @ExceptionHandler(NullPointerException.class)
    public String handleNullPointerException(NullPointerException e, RedirectAttributes redirectAttributes) {
        log.error("NullPointerException 발생: ", e);
        redirectAttributes.addFlashAttribute("error", "데이터를 찾을 수 없습니다.");
        return "redirect:/admin/dashboard";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e, RedirectAttributes redirectAttributes) {
        log.error("잘못된 파라미터: ", e);
        redirectAttributes.addFlashAttribute("error", "잘못된 요청입니다: " + e.getMessage());
        return "redirect:/admin/dashboard";
    }
}