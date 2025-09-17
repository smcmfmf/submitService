package kr.ac.kopo.smcmfmf.example.submitservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public String saveFile(MultipartFile file, String studentName) throws IOException {
        // 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("업로드 디렉토리 생성: {}", uploadPath.toAbsolutePath());
        }

        // 파일명 생성 (중복 방지를 위해 타임스탬프와 UUID 사용)
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = String.format("%s_%s_%s%s",
                studentName.replaceAll("[^a-zA-Z0-9가-힣]", "_"),
                timestamp,
                uniqueId,
                extension);

        Path filePath = uploadPath.resolve(fileName);

        // 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("파일 저장 완료: {} -> {}", originalFilename, fileName);

        return fileName; // 저장된 파일명 반환
    }

    public boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("파일 삭제 완료: {}", fileName);
            }
            return deleted;
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 발생: {}", fileName, e);
            return false;
        }
    }

    public boolean fileExists(String fileName) {
        Path filePath = Paths.get(uploadDir).resolve(fileName);
        return Files.exists(filePath);
    }
}