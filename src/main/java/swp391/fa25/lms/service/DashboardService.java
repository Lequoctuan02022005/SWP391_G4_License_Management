package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.DashboardRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    /**
     * Trả toàn bộ dữ liệu dashboard theo role
     */
    public Map<String, Object> getDashboardData(Account account) {

        Map<String, Object> data = new HashMap<>();

        Role.RoleName role = account.getRole().getRoleName();
        data.put("role", role.name());

        switch (role) {
            case ADMIN -> loadAdminDashboard(data);
            case SELLER -> loadSellerDashboard(data, account);
            case MANAGER -> loadManagerDashboard(data);
            case MOD -> loadModDashboard(data);
            default -> {
                // CUSTOMER / GUEST
            }
        }

        return data;
    }

    // =========================
    // ADMIN
    // =========================
    private void loadAdminDashboard(Map<String, Object> data) {

        data.put("totalAccounts", dashboardRepository.countAllAccounts());
        data.put("totalSellers", dashboardRepository.countAllSellers());
        data.put("totalTools", dashboardRepository.countAllTools());
        data.put("totalReports",
                dashboardRepository.countToolReports());
    }

    // =========================
    // SELLER
    // =========================
    private void loadSellerDashboard(Map<String, Object> data, Account seller) {

        Long sellerId = seller.getAccountId();

        // ===== KPI =====
        data.put("totalTools",
                dashboardRepository.countSellerTools(sellerId));

        data.put("publishedTools",
                dashboardRepository.countSellerPublishedTools(sellerId));

        data.put("pendingRejectedTools",
                dashboardRepository.countSellerPendingRejectedTools(sellerId));

        // Tổng doanh thu (ALL TIME) – từ CustomerOrder
        data.put("revenue",
                dashboardRepository.sumSellerRevenue(sellerId));

        // ===== TOOL STATUS BREAKDOWN =====
        List<Object[]> statusCounts =
                dashboardRepository.countSellerToolsByStatus(sellerId);

        // mặc định = 0 để dashboard KHÔNG BAO GIỜ bị trắng
        Map<Tool.Status, Long> statusMap = new EnumMap<>(Tool.Status.class);
        for (Tool.Status s : Tool.Status.values()) {
            statusMap.put(s, 0L);
        }

        for (Object[] row : statusCounts) {
            Tool.Status status = (Tool.Status) row[0];
            Long count = (Long) row[1];
            statusMap.put(status, count);
        }

        data.put("pendingCount", statusMap.get(Tool.Status.PENDING));
        data.put("approvedCount", statusMap.get(Tool.Status.APPROVED));
        data.put("rejectedCount", statusMap.get(Tool.Status.REJECTED));
        data.put("publishedCount", statusMap.get(Tool.Status.PUBLISHED));
        data.put("suspectCount", statusMap.get(Tool.Status.SUSPECT));
        data.put("deactivatedCount", statusMap.get(Tool.Status.DEACTIVATED));

        // ===== REVENUE LAST 5 MONTHS (FROM CUSTOMER_ORDER) =====
        List<Object[]> revenueRows =
                dashboardRepository.sumSellerRevenueByMonth(sellerId);

        List<String> revenueMonths = new ArrayList<>();
        List<BigDecimal> revenueValues = new ArrayList<>();

        revenueRows.stream()
                .limit(5)
                .forEach(r -> {

                    // r[0] = YEAR(o.createdAt)
                    // r[1] = MONTH(o.createdAt)
                    // r[2] = SUM(o.price)
                    Integer year = (Integer) r[0];
                    Integer month = (Integer) r[1];
                    Double totalDouble = (Double) r[2];
                    BigDecimal total = BigDecimal.valueOf(totalDouble);

                    revenueMonths.add(
                            year + "-" + String.format("%02d", month)
                    );
                    revenueValues.add(total);
                });

        data.put("revenueMonths", revenueMonths);
        data.put("revenueValues", revenueValues);

        // ===== SELLER PACKAGE =====
        Account sellerAccount =
                dashboardRepository.findSellerAccount(sellerId);

        if (sellerAccount.getSellerExpiryDate() != null) {

            long daysLeft = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    sellerAccount.getSellerExpiryDate().toLocalDate()
            );

            data.put("packageDaysLeft", daysLeft);

            if (daysLeft <= 0) {
                data.put("packageStatus", "EXPIRED");
            } else if (daysLeft <= 7) {
                data.put("packageStatus", "EXPIRING");
            } else {
                data.put("packageStatus", "ACTIVE");
            }

        } else {
            data.put("packageStatus", "NONE");
            data.put("packageDaysLeft", 0);
        }
    }

    // =========================
    // MANAGER
    // =========================
    private void loadManagerDashboard(Map<String, Object> data) {

        data.put("pendingTools",
                dashboardRepository.countPendingTools());

        data.put("suspectTools",
                dashboardRepository.countSuspectTools());

        data.put("publishedBlogs",
                dashboardRepository.countPublishedBlogs());

        data.put("topBlogs",
                dashboardRepository.findTopBlogs().stream().limit(5).toList());
    }

    // =========================
    // MOD
    // =========================
    private void loadModDashboard(Map<String, Object> data) {

        List<ToolReport> toolReports =
                dashboardRepository.findPendingToolReports();

        data.put("pendingToolReports", toolReports);

        data.put("totalPendingReports", toolReports.size());
    }
}
