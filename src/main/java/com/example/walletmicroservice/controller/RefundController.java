package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.dto.RefundApprovalDTO;
import com.example.walletmicroservice.dto.RefundRequestDTO;
import com.example.walletmicroservice.entity.PaymentTransaction;
import com.example.walletmicroservice.service.RazorpayService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
@Slf4j
public class RefundController {

    private final RazorpayService razorpayService;

    // ================================
    // STEP 1: REFUNDS
    // ================================

    /**
     * 5. Request Refund
     * POST /api/v1/payments/refund
     * Description: Request a refund for a successful payment
     */
    @PostMapping("/refund/initiate")
    public ResponseEntity<?> createRefund(@RequestBody RefundRequestDTO request) {
        try {
            PaymentTransaction refund = razorpayService.processRazorpayRefund(request);
            return ResponseEntity.ok(refund);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * 6. Get Pending Refunds (Admin)
     * GET /api/v1/payments/refund/pending
     * Description: Get list of pending refunds for approval
     */
    @GetMapping("/refund/pending")
    public ResponseEntity<?> getPendingRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PaymentTransaction> pendingRefunds = razorpayService.getPendingRefunds(page, size);
            return ResponseEntity.ok(pendingRefunds);
        } catch (Exception e) {
            log.error("Error fetching pending refunds", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching pending refunds");
        }
    }

    /**
     //     * POST /api/v1/payments/refund/approve
     //     * Requires ADMIN or FINANCE_MANAGER role
     //     */
    @PostMapping("/refund/approve")
    public ResponseEntity<?> approveRefund(@RequestBody RefundApprovalDTO approvalRequest) {
        try {
            RefundApprovalDTO refund = razorpayService.handleRefundApproved(approvalRequest);
            return ResponseEntity.ok(refund);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }


/**
 * POST /api/v1/payments/refund/request
 */
//    @PostMapping("/refund/request")
//    public ResponseEntity<?> requestRefund(@RequestBody RefundRequestDTO request) {
//        try {
//            PaymentTransaction refund = razorpayService.createRefund(request);
//            return ResponseEntity.ok(refund);
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(e.getMessage());
//        } catch (Exception e) {
//            log.error("Error creating refund request", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error creating refund request: " + e.getMessage());
//        }
//    }
//
//

/**
 * GET /api/v1/payments/refund/pending
 * Requires ADMIN or FINANCE_MANAGER role
 */


/**
 * GET /api/v1/payments/wallet/balance/{customerId}
 */
//    @GetMapping("/wallet/balance/{customerId}")
//    public ResponseEntity<?> getWalletBalance(@PathVariable String customerId) {
//        try {
//            BigDecimal balance = razorpayService.calculateWalletBalance(customerId);
//            return ResponseEntity.ok(balance);
//        } catch (Exception e) {
//            log.error("Error calculating wallet balance", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error calculating wallet balance");
//        }
//    }
}
