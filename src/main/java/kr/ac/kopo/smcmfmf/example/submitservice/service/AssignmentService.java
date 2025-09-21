package kr.ac.kopo.smcmfmf.example.submitservice.service;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.Assignment;
import kr.ac.kopo.smcmfmf.example.submitservice.domain.Course;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;

    public Assignment createAssignment(Assignment assignment) {
        // 마감일 검증
        validateDeadline(assignment.getDeadline());

        log.info("과제 생성: {} (마감일: {})", assignment.getTitle(), assignment.getDeadline());
        return assignmentRepository.save(assignment);
    }

    public Assignment updateAssignment(Assignment assignment) {
        // 마감일 검증 (기존 과제 수정 시에도 적용)
        validateDeadline(assignment.getDeadline());

        log.info("과제 수정: {} (마감일: {})", assignment.getTitle(), assignment.getDeadline());
        return assignmentRepository.save(assignment);
    }

    /**
     * 마감일 검증 메소드
     * @param deadline 설정하려는 마감일
     * @throws IllegalArgumentException 마감일이 유효하지 않은 경우
     */
    private void validateDeadline(LocalDateTime deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("마감일은 필수 입력 항목입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 과거 날짜 검증
        if (deadline.isBefore(now) || deadline.isEqual(now)) {
            log.warn("유효하지 않은 마감일 설정 시도: {} (현재 시간: {})", deadline, now);
            throw new IllegalArgumentException("마감일은 현재 시간 이후로 설정해야 합니다.");
        }

        // 너무 가까운 미래 경고 (1시간 이내)
        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, deadline);
        if (hoursUntilDeadline < 1) {
            log.warn("마감일이 1시간 이내로 설정됨: {} ({}시간 후)", deadline, hoursUntilDeadline);
            // 경고만 하고 허용 (비즈니스 요구사항에 따라 조정 가능)
        }

        // 너무 먼 미래 검증 (1년 이후)
        long daysUntilDeadline = ChronoUnit.DAYS.between(now, deadline);
        if (daysUntilDeadline > 365) {
            log.warn("마감일이 1년 이후로 설정됨: {} ({}일 후)", deadline, daysUntilDeadline);
            throw new IllegalArgumentException("마감일은 1년 이내로 설정해야 합니다.");
        }

        log.debug("마감일 검증 통과: {} ({}일 {}시간 후)",
                deadline,
                ChronoUnit.DAYS.between(now, deadline),
                ChronoUnit.HOURS.between(now, deadline) % 24);
    }

    /**
     * 마감일 검증을 위한 공개 메소드
     * @param deadline 검증할 마감일
     * @return 유효성 검사 결과
     */
    public boolean isValidDeadline(LocalDateTime deadline) {
        try {
            validateDeadline(deadline);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 마감일까지 남은 시간 계산
     * @param deadline 마감일
     * @return 남은 시간 정보
     */
    public String getTimeUntilDeadline(LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();

        if (deadline.isBefore(now)) {
            return "마감됨";
        }

        long days = ChronoUnit.DAYS.between(now, deadline);
        long hours = ChronoUnit.HOURS.between(now, deadline) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, deadline) % 60;

        if (days > 0) {
            return String.format("%d일 %d시간 남음", days, hours);
        } else if (hours > 0) {
            return String.format("%d시간 %d분 남음", hours, minutes);
        } else {
            return String.format("%d분 남음", minutes);
        }
    }

    // 기존 메소드들
    public List<Assignment> getAssignmentsByCourse(Course course) {
        return assignmentRepository.findByCourse(course);
    }

    public Assignment getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));
    }

    public void deleteAssignment(Long assignmentId) {
        Assignment assignment = getAssignmentById(assignmentId);
        log.info("과제 삭제: {}", assignment.getTitle());
        assignmentRepository.delete(assignment);
    }

    /**
     * 과제 마감일 연장
     * @param assignmentId 과제 ID
     * @param newDeadline 새로운 마감일
     * @return 수정된 과제
     */
    public Assignment extendDeadline(Long assignmentId, LocalDateTime newDeadline) {
        Assignment assignment = getAssignmentById(assignmentId);
        LocalDateTime originalDeadline = assignment.getDeadline();

        // 새로운 마감일 검증
        validateDeadline(newDeadline);

        // 기존 마감일보다 이후인지 확인
        if (newDeadline.isBefore(originalDeadline)) {
            throw new IllegalArgumentException("새로운 마감일은 기존 마감일보다 이후여야 합니다.");
        }

        assignment.setDeadline(newDeadline);
        log.info("과제 마감일 연장: {} ({} -> {})",
                assignment.getTitle(), originalDeadline, newDeadline);

        return assignmentRepository.save(assignment);
    }

    /**
     * 마감일이 임박한 과제 조회
     * @param course 과목
     * @param hours 임박 기준 시간
     * @return 임박한 과제 목록
     */
    public List<Assignment> getUpcomingAssignments(Course course, int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(hours);

        return assignmentRepository.findUpcomingAssignments(course, now, threshold);
    }

    /**
     * 마감된 과제 조회
     * @param course 과목
     * @return 마감된 과제 목록
     */
    public List<Assignment> getExpiredAssignments(Course course) {
        LocalDateTime now = LocalDateTime.now();
        return assignmentRepository.findByCourseAndDeadlineBefore(course, now);
    }
}