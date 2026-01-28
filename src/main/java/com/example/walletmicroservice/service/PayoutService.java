package com.example.walletmicroservice.service;

import com.example.walletmicroservice.dto.*;
import com.example.walletmicroservice.entity.PayoutTransaction;
import com.example.walletmicroservice.repository.PayoutTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.example.walletmicroservice.util.SecurityUtil.getCurrentUserId;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {

    @Value("${razorpay.payout.account_number}")
    private String razorpayPayoutAc;

    private final RazorpayHttpClientService razorpayHttpClient;
    private final PayoutTransactionRepository payoutRepository;
    private final LedgerService ledgerService;

    // 1. Create or Get Contact
    @Transactional
    public JSONObject createOrGetContact(ContactRequestDTO request) {
        JSONObject contactRequest = new JSONObject();
        contactRequest.put("name", request.getName());
        contactRequest.put("email", request.getEmail());
        contactRequest.put("contact", request.getMobile());
        contactRequest.put("type", request.getType());
        if (request.getReferenceId() != null) {
            contactRequest.put("reference_id", request.getReferenceId());
        }
        return razorpayHttpClient.createContact(contactRequest);
    }

    // 2. Create Fund Account
    @Transactional
    public JSONObject createFundAccount(FundAccountRequestDTO request) {
        // Create in Razorpay
        JSONObject fundAccountRequest = new JSONObject();
        fundAccountRequest.put("contact_id", request.getContactId());
        fundAccountRequest.put("account_type", request.getAccountType());

        JSONObject accountDetails = new JSONObject();
        if ("bank_account".equals(request.getAccountType())) {
            accountDetails.put("name", request.getAccountHolderName());
            accountDetails.put("ifsc", request.getIfscCode());
            accountDetails.put("account_number", request.getAccountNumber());
        } else if ("vpa".equals(request.getAccountType())) {
            accountDetails.put("address", request.getVpaAddress());
        }

        fundAccountRequest.put(request.getAccountType(), accountDetails);

        return razorpayHttpClient.createFundAccount(fundAccountRequest);
    }

    // 3. Initiate Payout
    @Transactional
    public PayoutTransaction initiatePayout(PayoutRequestDTO request) {
        // Check if payout already exists for this reference
        if (request.getReferenceId() != null) {
            Optional<PayoutTransaction> existingPayout = payoutRepository
                    .findAll().stream()
                    .filter(p -> p.getReferenceId() != null &&
                            p.getReferenceId().equals(request.getReferenceId()))
                    .findFirst();

            if (existingPayout.isPresent()) {
                throw new RuntimeException("Payout with this reference ID already exists");
            }
        }

        // Create payout transaction
        PayoutTransaction payout = new PayoutTransaction();
        payout.setPayoutId("POUT-" + UUID.randomUUID().toString().substring(0, 8));
        payout.setCustomerId(request.getCustomerId());
        payout.setContactId(request.getContactId());
        payout.setFundAccountId(request.getFundAccountId());
        payout.setAmount(request.getAmount());
        payout.setCurrency(request.getCurrency());
        payout.setMode(request.getMode());
        payout.setPurpose(request.getPurpose());
        payout.setReferenceId(request.getReferenceId());
        payout.setNarration(request.getNarration());
        payout.setStatus("created");
        payout.setCreatedUid(getCurrentUserId());
        payout.setUpdatedUid(getCurrentUserId());

        // Create in Razorpay
        JSONObject payoutRequest = new JSONObject();
        payoutRequest.put("account_number", razorpayPayoutAc);
        payoutRequest.put("fund_account_id", request.getFundAccountId());
        payoutRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        payoutRequest.put("currency", request.getCurrency());
        payoutRequest.put("mode", request.getMode());
        payoutRequest.put("purpose", request.getPurpose());

        if (request.getReferenceId() != null) {
            payoutRequest.put("reference_id", request.getReferenceId());
        }
        if (request.getNarration() != null) {
            payoutRequest.put("narration", request.getNarration());
        }
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            payoutRequest.put("notes", new JSONObject(request.getNotes()));
        }

        try {
            JSONObject razorpayResponse = razorpayHttpClient.createPayout(payoutRequest);
            payout.setRazorpayPayoutId(razorpayResponse.getString("id"));
            payout.setStatus(razorpayResponse.getString("status"));
            payout.setFees(BigDecimal.valueOf(razorpayResponse.optDouble("fees", 0))
                    .divide(BigDecimal.valueOf(100)));
            payout.setTax(BigDecimal.valueOf(razorpayResponse.optDouble("tax", 0))
                    .divide(BigDecimal.valueOf(100)));
            payout.setUtrNumber(razorpayResponse.optString("utr", null));

            // Store metadata
            JSONObject metadata = new JSONObject();
            metadata.put("razorpay_response", razorpayResponse);
            metadata.put("initiated_at", LocalDateTime.now().toString());
            payout.setMetadata(metadata.toString());

            ledgerService.recordWithdrawalProcessedSuccess(
                    razorpayResponse.getString("id"), request.getReferenceId(),
                    request.getCustomerId(), request.getFundAccountId(), request.getAmount(), getCurrentUserId());

        } catch (Exception e) {
            payout.setStatus("failed");
            payout.setFailureReason(e.getMessage());
            ledgerService.recordWithdrawalFailed(
                    request.getContactId(), request.getReferenceId(),
                    request.getCustomerId(), request.getFundAccountId(), request.getAmount(), getCurrentUserId());
            JSONObject metadata = new JSONObject();
            metadata.put("error", e.getMessage());
            metadata.put("failed_at", LocalDateTime.now().toString());
            payout.setMetadata(metadata.toString());

            log.error("Failed to create payout in Razorpay", e);
        }
        return payoutRepository.save(payout);
    }

    // 4. Process Payout Webhook
    @Transactional
    public void processPayoutWebhook(JSONObject webhookPayload) {
        String event = webhookPayload.getString("event");
        JSONObject payload = webhookPayload.getJSONObject("payload");
        JSONObject payout = payload.getJSONObject("payout");

        String razorpayPayoutId = payout.getString("id");
        String status = payout.getString("status");

        // Find payout transaction
        Optional<PayoutTransaction> payoutOpt = payoutRepository
                .findByRazorpayPayoutId(razorpayPayoutId);

        if (payoutOpt.isPresent()) {
            PayoutTransaction payoutTransaction = payoutOpt.get();
            payoutTransaction.setStatus(status);

            if (payout.has("utr")) {
                payoutTransaction.setUtrNumber(payout.getString("utr"));
            }
            if (payout.has("failure_reason")) {
                payoutTransaction.setFailureReason(payout.getString("failure_reason"));
            }
            if (payout.has("fees")) {
                payoutTransaction.setFees(BigDecimal.valueOf(payout.getDouble("fees"))
                        .divide(BigDecimal.valueOf(100)));
            }
            if (payout.has("tax")) {
                payoutTransaction.setTax(BigDecimal.valueOf(payout.getDouble("tax"))
                        .divide(BigDecimal.valueOf(100)));
            }

            if ("processed".equals(status)) {
                payoutTransaction.setProcessedAt(LocalDateTime.now());
            }

            // Update metadata
            JSONObject metadata = payoutTransaction.getMetadata() != null ?
                    new JSONObject(payoutTransaction.getMetadata()) : new JSONObject();
            metadata.put("webhook_event", event);
            metadata.put("webhook_payload", webhookPayload);
            metadata.put("updated_at", LocalDateTime.now().toString());
            payoutTransaction.setMetadata(metadata.toString());

            payoutRepository.save(payoutTransaction);
            log.info("Payout {} updated to status: {}", razorpayPayoutId, status);
        }
    }

    // 5. Get Payout Status
    public PayoutTransaction getPayoutStatus(String payoutId) {
        PayoutTransaction payout = payoutRepository.findByPayoutId(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        // If payout is still processing, fetch latest status from Razorpay
        if ("created".equals(payout.getStatus()) || "processing".equals(payout.getStatus())) {
            try {
                JSONObject razorpayStatus = razorpayHttpClient
                        .getPayoutStatus(payout.getRazorpayPayoutId());

                String newStatus = razorpayStatus.getString("status");
                if (!newStatus.equals(payout.getStatus())) {
                    payout.setStatus(newStatus);

                    if (razorpayStatus.has("utr")) {
                        payout.setUtrNumber(razorpayStatus.getString("utr"));
                    }
                    if ("processed".equals(newStatus)) {
                        payout.setProcessedAt(LocalDateTime.now());
                    }

                    payoutRepository.save(payout);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch payout status from Razorpay: {}", e.getMessage());
            }
        }

        return payout;
    }

    // 6. Get User Payouts
    public Page<PayoutTransaction> getUserPayouts(String customerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return payoutRepository.findByCustomerId(customerId, pageRequest);
    }

    // payout to wallets only digital amount
    @Transactional
    public PayoutResponseDTO payoutToWallet(PayoutResponseDTO request) {
        ledgerService.recordWalletPayout(request.getReferenceId(), request.getFundAccountId(),request.getCustomerId(),
                request.getContactId(),request.getAmount(), getCurrentUserId());
        return request;
    }

    @Transactional
    public WalletWithdrawal walletWithdrawalApproved(WalletWithdrawal request) {
        ledgerService.recordWithdrawalApproved(request.getReferenceId(), request.getFundAccountId(),request.getCustomerId(),
                request.getContactId(),request.getAmount(), getCurrentUserId());
        return request;
    }
}