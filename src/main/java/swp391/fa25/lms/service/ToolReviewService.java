package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolFile;
import swp391.fa25.lms.repository.ToolRepository;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ToolReviewService {

    private final FileStorageService fileStorageService;
    private final ToolRepository toolRepository;

    // ========================= LIST =========================
    public Page<Tool> getModeratorPendingTools(Long sellerId, Long categoryId, String keyword,
                                               LocalDateTime uploadFrom, LocalDateTime uploadTo,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return toolRepository.findModeratorPendingTools(sellerId, categoryId, keyword, uploadFrom, uploadTo, pageable);
    }

    public Page<Tool> getManagerApprovedTools(Long sellerId, Long categoryId, String keyword,
                                              LocalDateTime uploadFrom, LocalDateTime uploadTo,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return toolRepository.findManagerApprovedTools(sellerId, categoryId, keyword, uploadFrom, uploadTo, pageable);
    }

    // ========================= DETAIL =========================
    public Tool getToolOrThrow(Long toolId) {
        return toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found: " + toolId));
    }

    // ========================= ACTIONS =========================
    @Transactional
    public void moderatorApprove(Long toolId, Account moderatorAccount) {
        Tool tool = toolRepository.findByToolIdAndStatus(toolId, Tool.Status.PENDING)
                .orElseThrow(() -> new RuntimeException("Tool not found or not PENDING"));
        tool.setStatus(Tool.Status.APPROVED);
        tool.setReviewedBy(moderatorAccount.getRole().getRoleName().toString());
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setNote(null); // clear old note if you want
        toolRepository.save(tool);
    }

    @Transactional
    public void moderatorReject(Long toolId, String reason, Account moderatorAccount) {
        Tool tool = toolRepository.findByToolIdAndStatus(toolId, Tool.Status.PENDING)
                .orElseThrow(() -> new RuntimeException("Tool not found or not PENDING"));
        tool.setStatus(Tool.Status.REJECTED);
        tool.setReviewedBy(moderatorAccount.getRole().getRoleName().toString());
        tool.setNote(reason);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    @Transactional
    public void managerPublish(Long toolId, Account managerAccount) {
        Tool tool = toolRepository.findByToolIdAndStatus(toolId, Tool.Status.APPROVED)
                .orElseThrow(() -> new RuntimeException("Tool not found or not APPROVED"));
        if (tool.getAvailableQuantity() == null) {
            tool.setAvailableQuantity(tool.getQuantity());
        }
        tool.setStatus(Tool.Status.PUBLISHED);
        tool.setAvailableQuantity(tool.getQuantity());
        tool.setReviewedBy(managerAccount.getRole().getRoleName().toString());
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    // Manager set pending again (nếu muốn trả về mod)
    @Transactional
    public void managerSetPending(Long toolId, String note, Account managerAccount) {
        Tool tool = toolRepository.findByToolIdAndStatus(toolId, Tool.Status.APPROVED)
                .orElseThrow(() -> new RuntimeException("Tool not found or not APPROVED"));
        tool.setStatus(Tool.Status.PENDING);
        tool.setReviewedBy(managerAccount.getRole().getRoleName().toString());
        tool.setNote(note);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    @Transactional
    public void managerReject(Long toolId, String note, Account managerAccount) {
        Tool tool = toolRepository.findByToolIdAndStatus(toolId, Tool.Status.APPROVED)
                .orElseThrow(() -> new RuntimeException("Tool not found or not APPROVED"));
        tool.setStatus(Tool.Status.REJECTED);
        tool.setReviewedBy(managerAccount.getRole().getRoleName().toString());
        tool.setNote(note);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepository.save(tool);
    }

    public void modUploadWrappedFile(
            Long toolId,
            MultipartFile toolFile,
            Account mod
    ) {

        Tool tool = getToolOrThrow(toolId);

        try {
            // upload file y hệt seller
            String toolPath = fileStorageService.uploadToolFile(toolFile);

            ToolFile tf = new ToolFile();
            tf.setTool(tool);
            tf.setFilePath(toolPath);
            tf.setUploadedBy(mod);
            tf.setCreatedAt(LocalDateTime.now());
            tf.setFileType(ToolFile.FileType.WRAPPED);

            tool.getFiles().removeIf(f -> f.getFileType() == ToolFile.FileType.WRAPPED);
            tool.getFiles().add(tf);
            tool.setUpdatedAt(LocalDateTime.now());
            toolRepository.save(tool);

            // optional: giữ tool ở PENDING
            tool.setUpdatedAt(LocalDateTime.now());

            toolRepository.save(tool); // cascade save ToolFile

        } catch (IOException e) {
            throw new IllegalStateException("Upload wrapped file failed: " + e.getMessage());
        }
    }
}
