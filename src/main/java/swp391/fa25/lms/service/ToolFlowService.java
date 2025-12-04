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
    // 1️⃣ START CREATE TOOL (TOKEN)
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

        if (toolService.existsByToolName(tool.getToolName()))
            throw new IllegalArgumentException("Tool name already exists.");

        Category category = toolService.getCategoryById(categoryId);

        String img;
        String toolPath;

        try {
            img = fileStorageService.uploadImage(imageFile);
            toolPath = fileStorageService.uploadToolFile(toolFile);
        } catch (IOException e) {
            throw new IllegalStateException("File upload failed: " + e.getMessage());
        }
        tool.setImage(img);
        tool.setSeller(seller);
        tool.setCategory(category);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());

        // TOOL FILE ENTITY
        ToolFile toolFileEntity = new ToolFile();
        toolFileEntity.setTool(tool);
        toolFileEntity.setFilePath(toolPath);
        toolFileEntity.setUploadedBy(seller);
        toolFileEntity.setCreatedAt(LocalDateTime.now());
        toolFileEntity.setFileType(ToolFile.FileType.ORIGINAL);

        tool.setFiles(List.of(toolFileEntity));

        // BUILD LICENSE LIST
        List<License> licenses = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License lic = new License();
            lic.setName("License " + licenseDays.get(i) + " days");
            lic.setDurationDays(licenseDays.get(i));
            lic.setPrice(licensePrices.get(i));
            licenses.add(lic);
        }

        // LOGIN METHOD = USER PASSWORD → Save immediately
        if (tool.getLoginMethod() == Tool.LoginMethod.USER_PASSWORD) {
            Tool saved = toolService.createTool(tool, category);
            toolService.createLicensesForTool(saved, licenses);
            return;
        }

        // LOGIN METHOD = TOKEN → RANDOM TOKENS
        Set<String> existed = new HashSet<>();
        List<String> tokens = tokenService.randomList(tool.getQuantity(), existed);

        // SAVE SESSION
        session.setAttribute(SESSION_PENDING_TOOL,
                new ToolSessionData(tool, category, licenses, toolPath, tokens));
    }

    // ============================================================
    // 2️⃣ FINALIZE CREATE TOOL (TOKEN)
    // ============================================================
    public void finalizeTokenTool(List<String> tokens, HttpSession session) {

        ToolSessionData pending = (ToolSessionData) session.getAttribute(SESSION_PENDING_TOOL);
        if (pending == null)
            throw new IllegalStateException("Session expired.");

        Tool tool = pending.getTool();
        List<License> licenses = pending.getLicenses();

        // VALIDATE QUANTITY MATCH
        if (tokens.size() != tool.getQuantity())
            throw new IllegalArgumentException("Token count mismatch: expected "
                    + tool.getQuantity());

        // CHECK DUPLICATE TOKENS IN DB
        for (String t : tokens) {
            if (licenseAccountRepository.existsByToken(t))
                throw new IllegalArgumentException("Token already exists: " + t);
        }

        // SAVE TOOL
        Tool saved = toolService.createTool(tool, pending.getCategory());

        // SAVE LICENSES
        toolService.createLicensesForTool(saved, licenses);

        List<License> savedLic = licenseRepository.findByTool_ToolId(saved.getToolId());
        License primary = savedLic.get(0);

        // SAVE TOKEN ACCOUNTS
        for (String t : tokens) {
            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(primary);
            acc.setToken(t);
            acc.setUsed(false);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepository.save(acc);
        }

        session.removeAttribute(SESSION_PENDING_TOOL);
    }

    // ============================================================
    // 3️⃣ START EDIT TOOL SESSION (TOKEN)
    // ============================================================
    public void startEditToolSession(
            Tool existingTool,
            Tool updatedData,
            MultipartFile imageFile,
            MultipartFile toolFile,
            List<Integer> licenseDays,
            List<Double> licensePrices,
            HttpSession session
    ) {

        if (existingTool.getLoginMethod() != updatedData.getLoginMethod())
            throw new IllegalArgumentException("Login method cannot be changed.");

        // HANDLE IMAGE
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                updatedData.setImage(fileStorageService.uploadImage(imageFile));
            } catch (IOException e) {
                throw new IllegalStateException("Image upload failed: " + e.getMessage());
            }

        } else {
            updatedData.setImage(existingTool.getImage());
        }

        // HANDLE TOOL FILES
        List<ToolFile> newFiles = new ArrayList<>(existingTool.getFiles());

        if (toolFile != null && !toolFile.isEmpty()) {
            String newPath;
            try {
                newPath = fileStorageService.uploadToolFile(toolFile);
            } catch (IOException e) {
                throw new IllegalStateException("Tool file upload failed: " + e.getMessage());
            }
            ToolFile f = new ToolFile();
            f.setTool(existingTool);
            f.setFilePath(newPath);
            f.setUploadedBy(existingTool.getSeller());
            f.setCreatedAt(LocalDateTime.now());
            f.setFileType(ToolFile.FileType.ORIGINAL);

            newFiles.add(f);
        }

        updatedData.setFiles(newFiles);

        // BUILD NEW LICENSE LIST
        List<License> newLicenses = new ArrayList<>();
        for (int i = 0; i < licenseDays.size(); i++) {
            License l = new License();
            l.setName("License " + licenseDays.get(i) + " days");
            l.setDurationDays(licenseDays.get(i));
            l.setPrice(licensePrices.get(i));
            newLicenses.add(l);
        }

        // LOAD CURRENT TOKEN LIST
        List<LicenseAccount> acc = licenseAccountRepository.findByLicense_Tool_ToolId(existingTool.getToolId());
        List<String> tokens = acc.stream().map(LicenseAccount::getToken).collect(Collectors.toList());

        session.setAttribute(SESSION_PENDING_EDIT,
                new ToolSessionData(existingTool, existingTool.getCategory(), newLicenses, null, tokens));
    }

    // ============================================================
    // 4️⃣ FINALIZE EDIT TOOL (TOKEN)
    // ============================================================
    public void finalizeEditTokenTool(List<String> tokens, HttpSession session) {

        ToolSessionData pending = (ToolSessionData) session.getAttribute(SESSION_PENDING_EDIT);
        if (pending == null)
            throw new IllegalStateException("Session expired.");

        Tool tool = pending.getTool();

        if (tokens.size() != tool.getQuantity())
            throw new IllegalArgumentException("Token count mismatch.");

        // CHECK DUPLICATE
        for (String t : tokens) {
            LicenseAccount acc = licenseAccountRepository.findByToken(t);
            if (acc != null && !Objects.equals(acc.getLicense().getTool().getToolId(), tool.getToolId())) {
                throw new IllegalStateException("Token belongs to another tool: " + t);
            }
        }

        // UPDATE TOKENS
        tokenService.updateTokensForTool(tool, tokens);

        // UPDATE LICENSES + QUANTITY
        toolService.updateQuantityAndLicenses(
                tool.getToolId(),
                tokens.size(),
                pending.getLicenses()
        );

        session.removeAttribute(SESSION_PENDING_EDIT);
    }

    // ============================================================
    // CANCEL SESSION
    // ============================================================
    public void cancelToolCreation(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_TOOL);
        session.removeAttribute(SESSION_PENDING_EDIT);
    }

    // ============================================================
    // SESSION MODEL
    // ============================================================
    public static class ToolSessionData {
        private final Tool tool;
        private final Category category;
        private final List<License> licenses;
        private final String filePath;
        private final List<String> tokens;

        public ToolSessionData(Tool tool, Category category, List<License> licenses, String filePath, List<String> tokens) {
            this.tool = tool;
            this.category = category;
            this.licenses = licenses;
            this.filePath = filePath;
            this.tokens = tokens;
        }

        public Tool getTool() { return tool; }
        public Category getCategory() { return category; }
        public List<License> getLicenses() { return licenses; }
        public String getFilePath() { return filePath; }
        public List<String> getTokens() { return tokens; }
    }
}
