# 🎓 과제 제출 및 관리 시스템 (SubmitService)
이 프로젝트는 Spring Boot와 Thymeleaf를 기반으로 한 웹 애플리케이션으로, 교수, 학생, 관리자 3가지 역할로 분배된 과제 제출 및 관리 시스템입니다.

# 📋 프로젝트 개요
```
백엔드: Java 17, Spring Boot (MVC, Data JPA, Validation)
프론트엔드: Thymeleaf (서버 사이드 렌더링), HTML, CSS
핵심 기능:
    - 3가지 사용자 역할 (ADMIN, PROFESSOR, STUDENT) 기반의 권한 관리
    - 관리자: 사용자 회원가입 승인 및 계정 관리
    - 교수: 강좌 및 과제 생성, 마감일 연장, 학생 제출물 확인 및 채점
    - 학생: 강좌 수강 신청, 과제 확인 및 파일 제출, 성적 확인
데이터베이스: H2 (인메모리 DB)를 기본으로 사용 (DataInitializationConfig.java)
빌드 도구: Gradle
```

# 🧰 기술 스택
```
Backend: Java (17), Spring Boot
Frameworks: Spring MVC, Spring Data JPA, Spring Validation
Template Engine: Thymeleaf
Database: H2 Database (인메모리)
Build Tool: Gradle
Frontend: HTML, CSS
```

# ✨ 주요 기능 (역할별)

## 👤 관리자 (Admin)
```
최초 관리자 계정 설정: 시스템에 관리자가 없는 경우, /admin/setup을 통해 최초 관리자 계정을 생성할 수 있습니다.

사용자 관리: 회원가입을 요청한 사용자(교수, 학생) 목록을 확인하고, 계정을 승인하거나 거절할 수 있습니다. (/admin/pending)

전체 사용자 목록: 시스템에 등록된 모든 사용자를 조회하고 관리합니다.
```

## 👨‍🏫 교수 (Professor)
```
과목(Course) 관리: 담당 과목, 과제를 생성하고 학생의 과제물을 채점합니다.
```

## 3단계: 관리자 승인
```
http://localhost:8080/login에서 관리자 계정으로 로그인합니다.

관리자 대시보드에서 'Pending Users' (승인 대기 사용자) 메뉴로 이동합니다.

방금 가입한 교수/학생 계정을 'Approve'(승인)합니다.
```

## 4단계: 역할별 기능 사용
```
승인된 교수 또는 학생 계정으로 http://localhost:8080/login에서 로그인합니다.

각자의 대시보드에서 주요 기능을 사용할 수 있습니다.

교수: 강좌 생성 -> 과제 생성

학생: 강좌 수강신청 -> 과제 제출

교수: 제출물 확인 및 채점
```
