package swp391.fa25.lms.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    //  Thư mục lưu ảnh và file tool
    private static final String IMAGE_DIR = "uploads/images";
    private static final String TOOL_DIR = "uploads/tools";

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_TOOL_SIZE  = 200 * 1024 * 1024;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/jpg"
    );
    private static final List<String> ALLOWED_TOOL_EXTENSIONS = Arrays.asList(".zip", ".rar", ".exe");

    /**  Upload ảnh đại diện của tool */
    public String uploadImage(MultipartFile imageFile) throws IOException {
        if (imageFile.getSize() > MAX_IMAGE_SIZE)
            throw new IllegalArgumentException("Image exceeds maximum allowed size (5MB).");
        validateMimeType(imageFile, ALLOWED_IMAGE_TYPES, "image");
        return saveFile(imageFile, IMAGE_DIR);
    }

    /**  Upload file tool (.zip, .rar, .exe) */
    public String uploadToolFile(MultipartFile toolFile) throws IOException {
        if (toolFile.getSize() > MAX_IMAGE_SIZE)
            throw new IllegalArgumentException("Image exceeds maximum allowed size (5MB).");
        validateExtension(toolFile, ALLOWED_TOOL_EXTENSIONS, "tool file");
        return saveFile(toolFile, TOOL_DIR);
    }

    private void validateMimeType(MultipartFile file, List<String> allowed, String name) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Please upload a " + name + ".");

        if (!allowed.contains(file.getContentType()))
            throw new IllegalArgumentException("Invalid " + name + " format.");
    }

    private void validateExtension(MultipartFile file, List<String> allowed, String name) {
        String n = file.getOriginalFilename().toLowerCase();
        boolean ok = allowed.stream().anyMatch(n::endsWith);
        if (!ok)
            throw new IllegalArgumentException("Invalid " + name + " extension.");
    }

    private String saveFile(MultipartFile file, String dir) throws IOException {
        Path uploadDir = Paths.get(dir);
        Files.createDirectories(uploadDir);

        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String newFilename = UUID.randomUUID() + extension;

        Path target = uploadDir.resolve(newFilename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return dir + "/" + newFilename;
    }

}
