package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.CategoryRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ToolService {

    @Autowired private ToolRepository toolRepo;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private LicenseToolRepository licenseRepo;

    // ========== Finders ==========
    public boolean existsByToolName(String name) {
        return toolRepo.existsByToolName(name);
    }

    public Tool getToolById(Long id) {
        return toolRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found."));
    }

    public Tool getToolByIdAndSeller(Long id, Account seller) {
        return toolRepo.findByToolIdAndSeller(id, seller).orElse(null);
    }

    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
    }

    // ========== Create Tool ==========
    public Tool createTool(Tool tool, Category category) {
        tool.setCategory(category);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepo.save(tool);
    }

    public void createLicensesForTool(Tool tool, List<License> list) {
        for (License l : list) {
            l.setTool(tool);
            l.setCreatedAt(LocalDateTime.now());
            licenseRepo.save(l);
        }
    }

    // ========== Update Tool (USER_PASSWORD Mode) ==========
    public void updateTool(Long id,
                           Tool newData,
                           String imagePath,
                           String toolPath,
                           List<Integer> licenseDays,
                           List<Double> licensePrices,
                           Account seller) {

        Tool tool = getToolById(id);

        // BASIC INFO
        tool.setToolName(newData.getToolName());
        tool.setDescription(newData.getDescription());
        tool.setNote(newData.getNote());
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setStatus(Tool.Status.PENDING);

        // CATEGORY
        if (newData.getCategory() != null) {
            Category c = getCategoryById(newData.getCategory().getCategoryId());
            tool.setCategory(c);
        }

        // IMAGE
        if (imagePath != null && !imagePath.isBlank()) {
            tool.setImage(imagePath);
        }

        // TOOL FILE
        if (toolPath != null && !toolPath.isBlank()) {
            ToolFile f = new ToolFile();
            f.setTool(tool);
            f.setFilePath(toolPath);
            f.setFileType(ToolFile.FileType.ORIGINAL);
            f.setUploadedBy(seller);
            f.setCreatedAt(LocalDateTime.now());
            if (tool.getFiles() == null) tool.setFiles(new ArrayList<>());
            tool.getFiles().add(f);
        }

        // LICENSE LIST
        if (licenseDays != null && licensePrices != null && licenseDays.size() == licensePrices.size()) {
            List<License> old = licenseRepo.findByTool_ToolId(id);

            for (int i = 0; i < licenseDays.size(); i++) {
                License lic;

                if (i < old.size()) lic = old.get(i);
                else {
                    lic = new License();
                    lic.setTool(tool);
                    old.add(lic);
                }

                lic.setName("License " + licenseDays.get(i) + " days");
                lic.setDurationDays(licenseDays.get(i));
                lic.setPrice(licensePrices.get(i));

                licenseRepo.save(lic);
            }

            if (old.size() > licenseDays.size()) {
                for (int i = licenseDays.size(); i < old.size(); i++) {
                    licenseRepo.delete(old.get(i));
                }
            }
        }

        toolRepo.save(tool);
    }

    // ========== Update License + Quantity (TOKEN Mode) ==========
    public void updateQuantityAndLicenses(Long toolId, int qty, List<License> newLic) {
        Tool tool = getToolById(toolId);
        List<License> old = licenseRepo.findByTool_ToolId(toolId);

        for (int i = 0; i < newLic.size(); i++) {
            License src = newLic.get(i);
            License target;

            if (i < old.size()) target = old.get(i);
            else {
                target = new License();
                target.setTool(tool);
            }

            target.setName(src.getName());
            target.setDurationDays(src.getDurationDays());
            target.setPrice(src.getPrice());

            licenseRepo.save(target);
        }

        if (old.size() > newLic.size()) {
            for (int i = newLic.size(); i < old.size(); i++) {
                licenseRepo.delete(old.get(i));
            }
        }

        tool.setQuantity(qty);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepo.save(tool);
    }
}
