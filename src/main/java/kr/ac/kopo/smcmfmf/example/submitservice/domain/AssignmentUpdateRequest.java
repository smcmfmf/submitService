package kr.ac.kopo.smcmfmf.example.submitservice.domain;

import kr.ac.kopo.smcmfmf.example.submitservice.validation.FutureDeadline;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 과제 수정 요청 DTO
 */
@Data
public class AssignmentUpdateRequest {

    @NotNull(message = "과제 ID는 필수입니다.")
    private Long assignmentId;

    @NotBlank(message = "과제 제목은 필수 입력 항목입니다.")
    @Size(max = 200, message = "과제 제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "과제 설명은 필수 입력 항목입니다.")
    @Size(max = 2000, message = "과제 설명은 2000자 이하로 입력해주세요.")
    private String description;

    @NotNull(message = "마감일은 필수 입력 항목입니다.")
    @FutureDeadline(
            minMinutesFromNow = 0, // 수정 시에는 현재 시간과 같아도 허용 (기존 과제 보호)
            maxDaysFromNow = 365,
            message = "마감일은 현재 시간 이후, 1년 이내로 설정해야 합니다."
    )
    private LocalDateTime deadline;
}