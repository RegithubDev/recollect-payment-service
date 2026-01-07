package com.example.walletmicroservice.repository;

import com.example.walletmicroservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Transaction> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Transaction> findByUserId(Long userId);  // Keep this

    // REMOVE OR COMMENT OUT THIS METHOD since walletId no longer exists:
    // List<Transaction> findByWalletId(Long walletId);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    // Add findByReferenceId if you need it
    Optional<Transaction> findByReferenceId(String referenceId);

    // Find by parent transaction ID
    List<Transaction> findByParentTransactionId(String parentTransactionId);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findTransactionsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // REMOVE this method too since it uses walletId:
    // @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.walletId = :walletId AND t.status = 'COMPLETED'")
    // Optional<BigDecimal> getTotalCompletedAmountByWalletId(@Param("walletId") Long walletId);

    // Add method to get user's total completed amount instead:
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = 'COMPLETED'")
    Optional<BigDecimal> getTotalCompletedAmountByUserId(@Param("userId") Long userId);

    // Find transactions by user and type
    List<Transaction> findByUserIdAndType(Long userId, Transaction.TransactionType type);

    // Find transactions by user and status
    List<Transaction> findByUserIdAndStatus(Long userId, Transaction.TransactionStatus status);

    // Find recent transactions for a user
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactionsByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    // Find transactions by payment method
    List<Transaction> findByPaymentMethod(Transaction.PaymentMethod paymentMethod);

    // Find failed transactions for a user
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = 'FAILED'")
    List<Transaction> findFailedTransactionsByUserId(@Param("userId") Long userId);

    // Find completed transactions for a user within date range
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findCompletedTransactionsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}