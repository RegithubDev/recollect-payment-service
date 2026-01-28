package com.example.walletmicroservice.repository;

import com.example.walletmicroservice.entity.PayoutTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, Long> {
    Optional<PayoutTransaction> findByPayoutId(String payoutId);
    Optional<PayoutTransaction> findByRazorpayPayoutId(String razorpayPayoutId);
    Page<PayoutTransaction> findByCustomerId(String customerId, Pageable pageable);
}