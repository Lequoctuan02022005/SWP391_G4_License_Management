package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Account, Long> {

    // =========================
    // ADMIN
    // =========================

    @Query("SELECT COUNT(a) FROM Account a")
    long countAllAccounts();

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        WHERE a.role.roleName = 'SELLER'
    """)
    long countAllSellers();

    @Query("SELECT COUNT(t) FROM Tool t")
    long countAllTools();

    @Query("SELECT COUNT(tr) FROM ToolReport tr")
    long countToolReports();

    @Query("SELECT COUNT(fr) FROM FeedbackReport fr")
    long countFeedbackReports();

    // =========================
    // SELLER
    // =========================

    @Query("""
        SELECT COUNT(t)
        FROM Tool t
        WHERE t.seller.accountId = :sellerId
    """)
    long countSellerTools(@Param("sellerId") Long sellerId);

    @Query("""
        SELECT COUNT(t)
        FROM Tool t
        WHERE t.seller.accountId = :sellerId
          AND t.status = 'PUBLISHED'
    """)
    long countSellerPublishedTools(@Param("sellerId") Long sellerId);

    @Query("""
        SELECT COUNT(t)
        FROM Tool t
        WHERE t.seller.accountId = :sellerId
          AND t.status IN ('PENDING', 'REJECTED')
    """)
    long countSellerPendingRejectedTools(@Param("sellerId") Long sellerId);

    @Query("""
    SELECT COALESCE(SUM(o.price), 0)
    FROM CustomerOrder o
    WHERE o.tool.seller.accountId = :sellerId
      AND o.orderStatus = swp391.fa25.lms.model.CustomerOrder.OrderStatus.SUCCESS
""")
    Double sumSellerRevenue(@Param("sellerId") Long sellerId);

    @Query("""
        SELECT a
        FROM Account a
        WHERE a.accountId = :sellerId
    """)
    Account findSellerAccount(@Param("sellerId") Long sellerId);

    // ===== TOOL STATUS BREAKDOWN =====
    @Query("""
        SELECT t.status, COUNT(t)
        FROM Tool t
        WHERE t.seller.accountId = :sellerId
        GROUP BY t.status
    """)
    List<Object[]> countSellerToolsByStatus(@Param("sellerId") Long sellerId);

    // ===== REVENUE LAST 5 MONTHS =====
    // SELLER - revenue last 5 months (SQL Server compatible)
    // ===== SELLER REVENUE LAST 5 MONTHS (FROM CUSTOMER_ORDER) =====
    @Query("""
    SELECT
        YEAR(o.createdAt),
        MONTH(o.createdAt),
        SUM(o.price)
    FROM CustomerOrder o
    WHERE o.tool.seller.accountId = :sellerId
      AND o.orderStatus = swp391.fa25.lms.model.CustomerOrder.OrderStatus.SUCCESS
    GROUP BY YEAR(o.createdAt), MONTH(o.createdAt)
    ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC
""")
    List<Object[]> sumSellerRevenueByMonth(@Param("sellerId") Long sellerId);

    // =========================
    // MANAGER
    // =========================

    @Query("SELECT COUNT(t) FROM Tool t WHERE t.status = 'PENDING'")
    long countPendingTools();

    @Query("SELECT COUNT(t) FROM Tool t WHERE t.status = 'SUSPECT'")
    long countSuspectTools();

    @Query("SELECT COUNT(b) FROM Blog b WHERE b.status = 'PUBLISHED'")
    long countPublishedBlogs();

    @Query("""
        SELECT b
        FROM Blog b
        WHERE b.status = 'PUBLISHED'
        ORDER BY b.viewCount DESC
    """)
    List<Blog> findTopBlogs();

    // =========================
    // MOD
    // =========================

    @Query("""
        SELECT tr
        FROM ToolReport tr
        WHERE tr.status = 'PENDING'
    """)
    List<ToolReport> findPendingToolReports();

    @Query("""
        SELECT fr
        FROM FeedbackReport fr
        WHERE fr.status = 'PENDING'
    """)
    List<FeedbackReport> findPendingFeedbackReports();
}
