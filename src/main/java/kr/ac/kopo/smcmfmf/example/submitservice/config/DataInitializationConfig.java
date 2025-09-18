package kr.ac.kopo.smcmfmf.example.submitservice.config;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.*;
import kr.ac.kopo.smcmfmf.example.submitservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializationConfig implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            initializeData();
        }
    }

    private void initializeData() {
        log.info("초기 테스트 데이터를 생성합니다...");

        // 사용자 생성
        User professor1 = User.builder()
                .name("김교수")
                .email("prof@kopo.ac.kr")
                .password("password123")
                .role(User.Role.PROFESSOR)
                .build();

        User professor2 = User.builder()
                .name("이교수")
                .email("prof2@kopo.ac.kr")
                .password("password123")
                .role(User.Role.PROFESSOR)
                .build();

        User student1 = User.builder()
                .name("홍학생")
                .email("student1@kopo.ac.kr")
                .password("password123")
                .role(User.Role.STUDENT)
                .build();

        User student2 = User.builder()
                .name("박학생")
                .email("student2@kopo.ac.kr")
                .password("password123")
                .role(User.Role.STUDENT)
                .build();

        User student3 = User.builder()
                .name("최학생")
                .email("student3@kopo.ac.kr")
                .password("password123")
                .role(User.Role.STUDENT)
                .build();

        userRepository.save(professor1);
        userRepository.save(professor2);
        userRepository.save(student1);
        userRepository.save(student2);
        userRepository.save(student3);

        // 과목 생성
        Course course1 = Course.builder()
                .name("자바 프로그래밍")
                .code("JAVA101")
                .professor(professor1)
                .build();

        Course course2 = Course.builder()
                .name("데이터베이스")
                .code("DB201")
                .professor(professor1)
                .build();

        Course course3 = Course.builder()
                .name("웹 프로그래밍")
                .code("WEB301")
                .professor(professor2)
                .build();

        Course course4 = Course.builder()
                .name("알고리즘")
                .code("ALG401")
                .professor(professor2)
                .build();

        courseRepository.save(course1);
        courseRepository.save(course2);
        courseRepository.save(course3);
        courseRepository.save(course4);

        // 수강신청 생성
        enrollmentRepository.save(Enrollment.builder().student(student1).course(course1).build());
        enrollmentRepository.save(Enrollment.builder().student(student1).course(course2).build());
        enrollmentRepository.save(Enrollment.builder().student(student2).course(course1).build());
        enrollmentRepository.save(Enrollment.builder().student(student2).course(course3).build());
        enrollmentRepository.save(Enrollment.builder().student(student3).course(course2).build());
        enrollmentRepository.save(Enrollment.builder().student(student3).course(course3).build());

        // 과제 생성
        Assignment assignment1 = Assignment.builder()
                .course(course1)
                .title("자바 기초 문법")
                .description("변수, 조건문, 반복문을 활용한 간단한 프로그램을 작성하세요.")
                .deadline(LocalDateTime.of(2024, 12, 25, 23, 59))
                .attachmentUrl("/files/download/test.txt")
                .build();

        Assignment assignment2 = Assignment.builder()
                .course(course1)
                .title("OOP 실습")
                .description("클래스와 객체를 활용한 도서관 관리 시스템을 구현하세요.")
                .deadline(LocalDateTime.of(2024, 12, 30, 23, 59))
                .build();

        Assignment assignment3 = Assignment.builder()
                .course(course2)
                .title("ER 다이어그램 작성")
                .description("주어진 요구사항에 따라 ER 다이어그램을 작성하세요.")
                .deadline(LocalDateTime.of(2024, 12, 28, 23, 59))
                .attachmentUrl("/files/download/test.txt")
                .build();

        Assignment assignment4 = Assignment.builder()
                .course(course2)
                .title("SQL 쿼리 작성")
                .description("복잡한 조인과 서브쿼리를 포함한 SQL문을 작성하세요.")
                .deadline(LocalDateTime.of(2025, 1, 5, 23, 59))
                .build();

        Assignment assignment5 = Assignment.builder()
                .course(course3)
                .title("HTML/CSS 포트폴리오")
                .description("개인 포트폴리오 웹페이지를 만들어 제출하세요.")
                .deadline(LocalDateTime.of(2024, 12, 31, 23, 59))
                .build();

        Assignment assignment6 = Assignment.builder()
                .course(course3)
                .title("JavaScript 계산기")
                .description("JavaScript를 활용한 계산기를 구현하세요.")
                .deadline(LocalDateTime.of(2025, 1, 10, 23, 59))
                .build();

        assignmentRepository.save(assignment1);
        assignmentRepository.save(assignment2);
        assignmentRepository.save(assignment3);
        assignmentRepository.save(assignment4);
        assignmentRepository.save(assignment5);
        assignmentRepository.save(assignment6);

        // 제출물 생성
        Submission submission1 = Submission.builder()
                .assignment(assignment1)
                .student(student1)
                .fileUrl("/files/download/studentTest.txt")
                .submittedAt(LocalDateTime.of(2024, 12, 20, 15, 30))
                .grade(new BigDecimal("85.50"))
                .feedback("기본 문법은 잘 이해하셨네요. 코드 주석을 더 자세히 작성해주세요.")
                .build();

        Submission submission2 = Submission.builder()
                .assignment(assignment1)
                .student(student2)
                .fileUrl("/files/download/studentTest.txt")
                .submittedAt(LocalDateTime.of(2024, 12, 21, 10, 15))
                .grade(new BigDecimal("92.00"))
                .feedback("매우 잘 작성하셨습니다. 변수명이 명확해서 읽기 좋네요.")
                .build();

        Submission submission3 = Submission.builder()
                .assignment(assignment3)
                .student(student1)
                .fileUrl("/files/download/studentTest.txt")
                .submittedAt(LocalDateTime.of(2024, 12, 22, 18, 45))
                .build();

        Submission submission4 = Submission.builder()
                .assignment(assignment3)
                .student(student3)
                .fileUrl("/files/download/studentTest.txt")
                .submittedAt(LocalDateTime.of(2024, 12, 23, 14, 20))
                .grade(new BigDecimal("78.00"))
                .feedback("ER 다이어그램의 기본 구조는 좋습니다. 관계의 카디널리티 표현을 더 정확히 해주세요.")
                .build();

        Submission submission5 = Submission.builder()
                .assignment(assignment5)
                .student(student2)
                .fileUrl("/files/download/studentTest.txt")
                .submittedAt(LocalDateTime.of(2024, 12, 25, 16, 45))
                .grade(new BigDecimal("88.50"))
                .feedback("디자인이 깔끔하고 반응형도 잘 구현하셨네요. CSS 애니메이션을 추가하면 더 좋을 것 같습니다.")
                .build();

        Submission submission6 = Submission.builder()
                .assignment(assignment5)
                .student(student3)
                .fileUrl("/files/download/studentTest.txt")
                .submittedAt(LocalDateTime.of(2024, 12, 25, 16, 45))
                .grade(new BigDecimal("88.50"))
                .feedback("디자인이 깔끔하고 반응형도 잘 구현하셨네요. CSS 애니메이션을 추가하면 더 좋을 것 같습니다.")
                .build();

        submissionRepository.save(submission1);
        submissionRepository.save(submission2);
        submissionRepository.save(submission3);
        submissionRepository.save(submission4);
        submissionRepository.save(submission5);
        submissionRepository.save(submission6);

        log.info("===================================");
        log.info("테스트용 초기 데이터가 로딩되었습니다!");
        log.info("===================================");
        log.info("📚 교수 계정:");
        log.info("   이메일: prof@kopo.ac.kr");
        log.info("   비밀번호: password123");
        log.info("   이름: 김교수");
        log.info("");
        log.info("   이메일: prof2@kopo.ac.kr");
        log.info("   비밀번호: password123");
        log.info("   이름: 이교수");
        log.info("===================================");
        log.info("🎓 학생 계정:");
        log.info("   이메일: student1@kopo.ac.kr");
        log.info("   비밀번호: password123");
        log.info("   이름: 홍학생");
        log.info("");
        log.info("   이메일: student2@kopo.ac.kr");
        log.info("   비밀번호: password123");
        log.info("   이름: 박학생");
        log.info("");
        log.info("   이메일: student3@kopo.ac.kr");
        log.info("   비밀번호: password123");
        log.info("   이름: 최학생");
        log.info("===================================");
        log.info("📋 과목 코드들:");
        log.info("   JAVA101 - 자바 프로그래밍 (김교수)");
        log.info("   DB201 - 데이터베이스 (김교수)");
        log.info("   WEB301 - 웹 프로그래밍 (이교수)");
        log.info("   ALG401 - 알고리즘 (이교수)");
        log.info("===================================");
        log.info("💻 H2 Console: http://localhost:8080/h2-console");
        log.info("   JDBC URL: jdbc:h2:mem:schooldb");
        log.info("   Username: sa");
        log.info("   Password: (비워두기)");
        log.info("===================================");
    }
}