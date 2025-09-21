package kr.ac.kopo.smcmfmf.example.submitservice.domain;

import kr.ac.kopo.smcmfmf.example.submitservice.validation.FutureDeadline;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 과제 생성 요청 DTO
 */
@Data
public class AssignmentCreateRequest {

    @NotBlank(message = "과제 제목은 필수 입력 항목입니다.")
    @Size(max = 200, message = "과제 제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "과제 설명은 필수 입력 항목입니다.")
    @Size(max = 2000, message = "과제 설명은 2000자 이하로 입력해주세요.")
    private String description;

    @NotNull(message = "마감일은 필수 입력 항목입니다.")
    @FutureDeadline(
            minMinutesFromNow = 60, // 최소 1시간 이후
            maxDaysFromNow = 365,   // 최대 1년 이내
            message = "마감일은 현재 시간으로부터 1시간 이후, 1년 이내로 설정해야 합니다."
    )
    private LocalDateTime deadline;

    @NotNull(message = "과목 ID는 필수입니다.")
    private Long courseId;
}