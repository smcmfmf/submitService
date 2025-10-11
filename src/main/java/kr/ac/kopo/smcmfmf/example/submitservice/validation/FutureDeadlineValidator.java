package kr.ac.kopo.smcmfmf.example.submitservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

/**
 * FutureDeadline 어노테이션을 위한 검증 클래스
 */
public class FutureDeadlineValidator implements ConstraintValidator<FutureDeadline, LocalDateTime> {

    private int minMinutesFromNow;
    private int maxDaysFromNow;

    @Override
    public void initialize(FutureDeadline constraintAnnotation) {
        this.minMinutesFromNow = constraintAnnotation.minMinutesFromNow();
        this.maxDaysFromNow = constraintAnnotation.maxDaysFromNow();
    }

    @Override
    public boolean isValid(LocalDateTime deadline, ConstraintValidatorContext context) {
        if (deadline == null) {
            return true; // null은 @NotNull로 처리
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minAllowedTime = now.plusMinutes(minMinutesFromNow);
        LocalDateTime maxAllowedTime = now.plusDays(maxDaysFromNow);

        // 최소 시간 검증
        if (deadline.isBefore(minAllowedTime)) {
            // 커스텀 메시지 설정
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("마감일은 현재 시간으로부터 최소 %d분 이후로 설정해야 합니다.", minMinutesFromNow)
            ).addConstraintViolation();
            return false;
        }

        // 최대 시간 검증
        if (deadline.isAfter(maxAllowedTime)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("마감일은 현재 시간으로부터 최대 %d일 이내로 설정해야 합니다.", maxDaysFromNow)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}