package kr.ac.kopo.smcmfmf.example.submitservice.repository;

import kr.ac.kopo.smcmfmf.example.submitservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // 로그인 시 이메일로 조회
}
