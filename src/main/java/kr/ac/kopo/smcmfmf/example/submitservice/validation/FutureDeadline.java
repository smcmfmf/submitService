// src/main/java/kr/ac/kopo/smcmfmf/example/submitservice/validation/FutureDeadline.java

package kr.ac.kopo.smcmfmf.example.submitservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 마감일이 현재 시간 이후인지 검증하는 커스텀 어노테이션
 */
@Documented
@Constraint(validatedBy = FutureDeadlineValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface FutureDeadline {

    String message() default "마감일은 현재 시간 이후로 설정해야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 최소 여유 시간 (분 단위)
     * 기본값: 60분 (1시간)
     */
    int minMinutesFromNow() default 60;

    /**
     * 최대 허용 기간 (일 단위)
     * 기본값: 365일 (1년)
     */
    int maxDaysFromNow() default 365;
}