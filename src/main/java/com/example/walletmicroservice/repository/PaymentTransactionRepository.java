package com.example.walletmicroservice.repository;

import com.example.walletmicroservice.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // Basic CRUD operations
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    List<PaymentTransaction> findAllByRazorpayOrderId(String razorpayOrderId);
    Page<PaymentTransaction> findByCustomerId(String customerId, Pageable pageable);

    List<PaymentTransaction> findAllByCustomerId(String customerId);

    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<PaymentTransaction> findByRazorpayRefundId(String razorpayRefundId);

    // Refund related queries
    Page<PaymentTransaction> findByRefundApprovalStatus(
            PaymentTransaction.RefundApprovalStatus status,
            Pageable pageable);

    List<PaymentTransaction> findByCustomerIdAndTransactionType(
            String customerId,
            PaymentTransaction.TransactionType transactionType);

    // Find by multiple criteria
    @Query("SELECT p FROM PaymentTransaction p WHERE " +
            "(:customerId IS NULL OR p.customerId = :customerId) AND " +
            "(:orderId IS NULL OR p.orderId = :orderId) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:transactionType IS NULL OR p.transactionType = :transactionType)")
    Page<PaymentTransaction> findByCriteria(
            @Param("customerId") String customerId,
            @Param("orderId") String orderId,
            @Param("status") PaymentTransaction.TransactionStatus status,
            @Param("transactionType") PaymentTransaction.TransactionType transactionType,
            Pageable pageable);

    // Find pending refunds for a specific customer
    Page<PaymentTransaction> findByCustomerIdAndRefundApprovalStatus(
            String customerId,
            PaymentTransaction.RefundApprovalStatus status,
            Pageable pageable);

    // Find refunds by original transaction
    Optional<PaymentTransaction> findByOriginalTransactionId(String originalTransactionId);

    // Find all refunds for a payment
    List<PaymentTransaction> findByOriginalTransactionIdAndTransactionType(
            String originalTransactionId,
            PaymentTransaction.TransactionType transactionType);

    // Find successful payments for a customer
    List<PaymentTransaction> findByCustomerIdAndStatusAndTransactionType(
            String customerId,
            PaymentTransaction.TransactionStatus status,
            PaymentTransaction.TransactionType transactionType);

    // Find recent transactions
    List<PaymentTransaction> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

    // Sum of amounts for a customer (for wallet balance calculation)
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE " +
            "p.customerId = :customerId AND " +
            "p.transactionType = :transactionType AND " +
            "p.status = :status")
    BigDecimal sumAmountByCustomerAndTypeAndStatus(
            @Param("customerId") String customerId,
            @Param("transactionType") PaymentTransaction.TransactionType transactionType,
            @Param("status") PaymentTransaction.TransactionStatus status);

    // Check if refund already exists for a payment
    @Query("SELECT COUNT(p) > 0 FROM PaymentTransaction p WHERE " +
            "p.originalTransactionId = :originalTransactionId AND " +
            "p.transactionType = 'REFUND' AND " +
            "p.refundStatus = 'PROCESSED'")
    boolean existsProcessedRefundForTransaction(@Param("originalTransactionId") String originalTransactionId);
}