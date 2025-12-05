package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TokenService {

    @Autowired private LicenseAccountRepository licenseAccountRepository;
    @Autowired private LicenseToolRepository licenseRepository;

    private final Random random = new Random();

    /** Random 6-digit token */
    public String randomToken() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /** Random unique token (not duplicate in session or DB) */
    public String randomUnique(Set<String> existed) {
        String token;
        do {
            token = randomToken();
        } while (existed.contains(token) || licenseAccountRepository.existsByToken(token));
        return token;
    }

    /** Generate N tokens */
    public List<String> randomList(int qty, Set<String> existed) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            String t = randomUnique(existed);
            existed.add(t);
            result.add(t);
        }
        return result;
    }

    /** Update token list for EDIT TOOL (TOKEN login) */
    public void updateTokensForTool(Tool tool, List<String> newTokens) {

        List<LicenseAccount> old = licenseAccountRepository.findByLicense_Tool_ToolId(tool.getToolId());
        Set<String> oldSet = old.stream().map(LicenseAccount::getToken).collect(Collectors.toSet());

        List<License> licenses = licenseRepository.findByTool_ToolId(tool.getToolId());
        if (licenses.isEmpty())
            throw new IllegalStateException("Tool has no license.");

        License primary = licenses.get(0);

        // DELETE
        for (LicenseAccount acc : old) {
            if (!newTokens.contains(acc.getToken())) {
                licenseAccountRepository.delete(acc);
            }
        }

        // ADD NEW
        for (String token : newTokens) {
            if (oldSet.contains(token)) continue;

            LicenseAccount acc = new LicenseAccount();
            acc.setLicense(primary);
            acc.setToken(token);
            acc.setUsed(false);
            acc.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepository.save(acc);
        }
    }
}

