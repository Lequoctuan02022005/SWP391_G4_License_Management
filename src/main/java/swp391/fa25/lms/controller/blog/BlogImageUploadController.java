package swp391.fa25.lms.controller.blog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller xử lý upload ảnh cho Blog
 * Chỉ MANAGER có quyền upload
 */
@RestController
@RequestMapping("/api/manager/blogs")
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
public class BlogImageUploadController {

    private static final String UPLOAD_DIR = "uploads/images/blogs/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * Upload ảnh thumbnail
     * POST /api/manager/blogs/upload/thumbnail
     */
    @PostMapping("/upload/thumbnail")
    public ResponseEntity<?> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        return uploadImage(file, "thumbnail");
    }

    /**
     * Upload ảnh banner
     * POST /api/manager/blogs/upload/banner
     */
    @PostMapping("/upload/banner")
    public ResponseEntity<?> uploadBanner(@RequestParam("file") MultipartFile file) {
        return uploadImage(file, "banner");
    }

    /**
     * Xử lý upload ảnh chung
     */
    private ResponseEntity<?> uploadImage(MultipartFile file, String type) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("message", "File quá lớn. Tối đa 5MB");
                return ResponseEntity.badRequest().body(response);
            }

            // Check file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên file không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }

            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            if (!isAllowedExtension(fileExtension)) {
                response.put("success", false);
                response.put("message", "Định dạng file không được hỗ trợ. Chỉ chấp nhận: jpg, jpeg, png, gif, webp");
                return ResponseEntity.badRequest().body(response);
            }

            // Create upload directory if not exists
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String newFilename = String.format("%s_%s_%s.%s", type, timestamp, uniqueId, fileExtension);

            // Save file
            Path filePath = Paths.get(UPLOAD_DIR + newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Generate URL path (relative to static resources)
            String fileUrl = "/" + UPLOAD_DIR + newFilename;

            log.info("Image uploaded successfully: {} (type: {})", newFilename, type);

            response.put("success", true);
            response.put("message", "Upload ảnh thành công");
            response.put("filename", newFilename);
            response.put("url", fileUrl);
            response.put("size", file.getSize());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error uploading image", e);
            response.put("success", false);
            response.put("message", "Lỗi khi upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy extension của file
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Kiểm tra extension có được phép không
     */
    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
