package swp391.fa25.lms.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ToolFlowService {

    @Autowired private ToolService toolService;
    @Autowired private TokenService tokenService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private LicenseAccountRepository licenseAccountRepository;
    @Autowired private LicenseToolRepository licenseRepository;

    private static final String SESSION_PENDING_TOOL = "pendingTool";
    private static final String SESSION_PENDING_EDIT = "pendingEditTool";

    // ============================================================
    // 1️ START CREATE TOOL (TOKEN)
    // ============================================================
    public void startCreateTool(
            Tool tool,
            MultipartFile imageFile,
            MultipartFile toolFile,
            Long categoryId,
            List<Integer> licenseDays,
            List<Double> licensePrices,
            HttpSession session
    ) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null)
            throw new IllegalStateException("Please login again.");

        Category category = toolService.getCategoryById(categoryId);

        try {
            tool.setImage(fileStorageService.uploadImage(imageFile));
            String toolPath = fileStorageService.uploadToolFile(toolFile);

            ToolFile tf = new ToolFile();
            tf.setTool(tool);
            tf.setFilePath(toolPath);
            tf.setUploadedBy(seller);
            tf.setCreatedAt(LocalDateTime.now());
            tf.setFileType(ToolFile.FileType.ORIGINAL);
            tool.setFiles(List.of(tf));

        } catch (IOException e) {
            throw new IllegalStateException("File upload failed: " + e.getMessage());
        }

        tool.setSeller(seller);
        tool.setCategory(category);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());

        // BUILD LICENSE LIST
        List<License> licenses = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License l = new License();
            l.setName("License " + licenseDays.get(i) + " days");
            l.setDurationDays(licenseDays.get(i));
            l.setPrice(licensePrices.get(i));
            licenses.add(l);
        }

        // USERNAME/PASSWORD → SAVE NGAY
        if (tool.getLoginMethod() == Tool.LoginMethod.USER_PASSWORD) {
            Tool saved = toolService.createTool(tool, category);
            toolService.createLicensesForTool(saved, licenses);
            return;
        }

        // TOKEN MODE → LƯU SESSION
        List<String> tokens = tokenService.randomList(tool.getQuantity(), new HashSet<>());

        session.setAttribute(
                SESSION_PENDING_TOOL,
                new ToolSessionData(tool, category, licenses, tokens)
        );
    }

    // ============================================================
    // 2️ FINALIZE CREATE TOOL (TOKEN)
    // ============================================================
    public void finalizeTokenTool(List<String> tokens, HttpSession session) {

        ToolSessionData pending =
                (ToolSessionData) session.getAttribute(SESSION_PENDING_TOOL);
        if (pending == null)
            throw new IllegalStateException("Session expired.");

        Tool tool = pending.getTool();

        // quantity = số token
        tool.setQuantity(tokens.size());
        tool.setAvailableQuantity(0);
        tool.setStatus(Tool.Status.PENDING);
        tool.setUpdatedAt(LocalDateTime.now());

        // SAVE TOOL
        Tool saved = toolService.createTool(tool, pending.getCategory());

        // SAVE LICENSE
        toolService.createLicensesForTool(saved, pending.getLicenses());

        // SAVE TOKENS
        License primary = licenseRepository
                .findByTool_ToolId(saved.getToolId())
                .get(0);

        for (String t : tokens) {
            if (licenseAccountRepository.existsByToken(t))
                throw new IllegalArgumentException("Token already exists: " + t);

            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(primary);
            acc.setToken(t);
            acc.setUsed(false);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepository.save(acc);
        }

        //  CỰC KỲ QUAN TRỌNG
        session.removeAttribute(SESSION_PENDING_TOOL);
    }
    // ============================================================
    // 3️ START EDIT TOOL SESSION (TOKEN)
    // ============================================================
    public void startEditToolSession(
            Long toolId,
            List<Integer> licenseDays,
            List<Double> licensePrices,
            HttpSession session
    ) {

        Tool tool = toolService.getToolById(toolId);

        List<License> newLicenses = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License l = new License();
            l.setName("License " + licenseDays.get(i) + " days");
            l.setDurationDays(licenseDays.get(i));
            l.setPrice(licensePrices.get(i));
            newLicenses.add(l);
        }

        List<String> tokens = licenseAccountRepository
                .findByLicense_Tool_ToolId(toolId)
                .stream()
                .map(LicenseAccount::getToken)
                .collect(Collectors.toList());

        session.setAttribute(
                SESSION_PENDING_EDIT,
                new ToolSessionData(tool, tool.getCategory(), newLicenses, tokens)
        );
    }

    // ============================================================
    // 4️ FINALIZE EDIT TOOL (TOKEN)
    // ============================================================
    public void finalizeEditTokenTool(List<String> tokens, HttpSession session) {

        ToolSessionData pending =
                (ToolSessionData) session.getAttribute(SESSION_PENDING_EDIT);
        if (pending == null)
            throw new IllegalStateException("Session expired.");

        Tool tool = pending.getTool();

        tool.setToolName(pending.getTool().getToolName());
        tool.setDescription(pending.getTool().getDescription());
        tool.setNote(pending.getTool().getNote());
        tool.setCategory(pending.getCategory());

        for (String t : tokens) {
            LicenseAccount acc = licenseAccountRepository.findByToken(t);
            if (acc != null &&
                    !Objects.equals(acc.getLicense().getTool().getToolId(), tool.getToolId())) {
                throw new IllegalStateException("Token belongs to another tool: " + t);
            }
        }

        tokenService.updateTokensForTool(tool, tokens);

        toolService.updateQuantityAndLicenses(
                tool.getToolId(),
                tokens.size(),
                pending.getLicenses()
        );

        session.removeAttribute(SESSION_PENDING_EDIT);
    }

    // ============================================================
    // CANCEL
    // ============================================================
    public void cancelToolCreation(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_TOOL);
        session.removeAttribute(SESSION_PENDING_EDIT);
    }

    // ============================================================
    // SESSION DTO
    // ============================================================
    public static class ToolSessionData {
        private final Tool tool;
        private final Category category;
        private final List<License> licenses;
        private final List<String> tokens;

        public ToolSessionData(
                Tool tool,
                Category category,
                List<License> licenses,
                List<String> tokens
        ) {
            this.tool = tool;
            this.category = category;
            this.licenses = licenses;
            this.tokens = tokens;
        }

        public Tool getTool() { return tool; }
        public Category getCategory() { return category; }
        public List<License> getLicenses() { return licenses; }
        public List<String> getTokens() { return tokens; }
    }
}
