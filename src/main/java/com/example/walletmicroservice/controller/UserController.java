package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.entity.PaymentTransaction;
import com.example.walletmicroservice.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final RazorpayService razorpayService;

    // 1. Paginated API
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Page<PaymentTransaction> transactions = razorpayService
                .getUserTransactions(userId, page, size, sortBy, sortDirection);

        return ResponseEntity.ok(transactions);
    }

    // 2. Recent API (simple)
    @GetMapping("/{userId}/transactions/recent")
    public ResponseEntity<?> getRecentUserTransactions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {

        List<PaymentTransaction> recentTransactions = razorpayService
                .getRecentUserTransactions(userId, limit);

        return ResponseEntity.ok(recentTransactions);
    }
}