package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.dto.*;
import com.example.walletmicroservice.entity.PayoutTransaction;
import com.example.walletmicroservice.service.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
@Slf4j
public class PayoutController {

    private final PayoutService payoutService;

    // ================= CONTACT APIS =================

    @PostMapping("/contacts")
    public ResponseEntity<?> createContact(@RequestBody ContactRequestDTO request) {
        try {
            JSONObject contact = payoutService.createOrGetContact(request);
            return ResponseEntity.ok(contact.toString());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating contact");
        }
    }

    // ================= FUND ACCOUNT APIS =================

    @PostMapping("/fund-accounts")
    public ResponseEntity<?> createFundAccount(@RequestBody FundAccountRequestDTO request) {
        try {
            JSONObject fundAccount = payoutService.createFundAccount(request);
            return ResponseEntity.ok(fundAccount.toString());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating fund account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating fund account");
        }
    }

    // ================= Wallet APIS =================

    @PostMapping("/to/wallet")
    public ResponseEntity<?> toWallet(@RequestBody PayoutResponseDTO request) {
        try {
            PayoutResponseDTO payout = payoutService.payoutToWallet(request);
            return ResponseEntity.ok(payout);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error initiating payout to wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initiating payout to wallet");
        }
    }

    @PostMapping("/wallet/approve/withdraw")
    public ResponseEntity<?> walletApprovedWithdraw(@RequestBody WalletWithdrawal request) {
        try {
            WalletWithdrawal approve = payoutService.walletWithdrawalApproved(request);
            return ResponseEntity.ok(approve);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error wallet withdrawal approval process", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error wallet withdrawal approval process");
        }
    }
    // ================= PAYOUT APIS =================
    @GetMapping("/status/{payoutId}")
    public ResponseEntity<?> getPayoutStatus(@PathVariable String payoutId) {
        try {
            PayoutTransaction payout = payoutService.getPayoutStatus(payoutId);
            return ResponseEntity.ok(payout);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching payout status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching payout status");
        }
    }

    @GetMapping("/user/{customerId}")
    public ResponseEntity<?> getUserPayouts(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PayoutTransaction> payouts = payoutService.getUserPayouts(customerId, page, size);
            return ResponseEntity.ok(payouts);
        } catch (Exception e) {
            log.error("Error fetching user payouts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching payouts");
        }
    }

    // ================= WEBHOOK =================

    @PostMapping("/webhook")
    public ResponseEntity<?> handlePayoutWebhook(
            @RequestBody String payload) {
        try {
            JSONObject webhookPayload = new JSONObject(payload);
            payoutService.processPayoutWebhook(webhookPayload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing payout webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}