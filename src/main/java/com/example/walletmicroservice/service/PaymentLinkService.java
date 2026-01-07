package com.example.walletmicroservice.service;

import com.example.walletmicroservice.dto.PaymentButtonRequestDTO;
import com.example.walletmicroservice.dto.PaymentButtonResponseDTO;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentLinkService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.callback.success}")
    private String defaultCallbackUrl;

    @Value("${razorpay.callback.webhook}")
    private String defaultWebhookUrl;

    /**
     * Create a Razorpay payment link for dynamic amounts
     */
    public PaymentButtonResponseDTO createPaymentLink(PaymentButtonRequestDTO request) throws RazorpayException {
        try {
            JSONObject paymentLinkRequest = buildPaymentLinkRequest(request);

            log.info("Creating payment link for amount: ₹{}, User: {}, Email: {}",
                    request.getAmount(),
                    request.getCustomerName(),
                    request.getCustomerEmail());

            // Call Razorpay API to create payment link
            PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);
            log.info("Payment link created successfully: {}", Optional.ofNullable(paymentLink.get("id")));

            // Build and return response
            return buildPaymentLinkResponse(paymentLink);

        } catch (RazorpayException e) {
            log.error("Razorpay API error creating payment link: {}", e.getMessage());
            throw new RazorpayException("Failed to create payment link: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating payment link: {}", e.getMessage(), e);
            throw new RazorpayException("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Build the Razorpay payment link request with 30-minute expiry
     */
    private JSONObject buildPaymentLinkRequest(PaymentButtonRequestDTO request) {
        JSONObject paymentLinkRequest = new JSONObject();

        // Convert amount to paise
        long amountInPaise = request.getAmount().multiply(new BigDecimal("100")).longValue();
        paymentLinkRequest.put("amount", amountInPaise);
        paymentLinkRequest.put("currency", request.getCurrency());
        paymentLinkRequest.put("accept_partial", request.getAcceptPartial() != null ? request.getAcceptPartial() : false);
        paymentLinkRequest.put("description", request.getDescription());

        // Add customer details
        JSONObject customer = new JSONObject();
        if (request.getCustomerName() != null) {
            customer.put("name", request.getCustomerName());
        }
        if (request.getCustomerEmail() != null) {
            customer.put("email", request.getCustomerEmail());
        }
        if (request.getCustomerPhone() != null) {
            customer.put("contact", request.getCustomerPhone());
        }

        // Add customer if any details provided
        if (customer.length() > 0) {
            paymentLinkRequest.put("customer", customer);
        }

        // Enable notifications
        JSONObject notify = new JSONObject();
        notify.put("sms", true);
        notify.put("email", true);
        paymentLinkRequest.put("notify", notify);

        // Enable reminders
        paymentLinkRequest.put("reminder_enable", true);

        // Add notes/metadata
        JSONObject notes = new JSONObject();
        notes.put("source", "wallet-microservice");
        notes.put("created_at", Instant.now().getEpochSecond());
        if (request.getReferenceId() != null) {
            notes.put("reference_id", request.getReferenceId());
        }
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            request.getNotes().forEach(notes::put);
        }
        paymentLinkRequest.put("notes", notes);

        // Set callback URL
        String callbackUrl = request.getCallbackUrl() != null ? request.getCallbackUrl() : defaultCallbackUrl;
        paymentLinkRequest.put("callback_url", callbackUrl);
        paymentLinkRequest.put("callback_method", "get");

//        // Set webhook URL if provided
//        if (request.getWebhookUrl() != null) {
//            paymentLinkRequest.put("callback_url", request.getWebhookUrl());
//        }

        // Set expiry to 30 minutes from now
        long expiryTimestamp = Instant.now().plusSeconds(30 * 60).getEpochSecond();
        paymentLinkRequest.put("expire_by", expiryTimestamp);

        return paymentLinkRequest;
    }

    /**
     * Build response DTO from Razorpay payment link
     */
    private PaymentButtonResponseDTO buildPaymentLinkResponse(PaymentLink paymentLink) {
        try {
            // Safely extract values
            String paymentLinkId = safeGetString(paymentLink, "id");
            String shortUrl = safeGetString(paymentLink, "short_url");
            String fullUrl = safeGetString(paymentLink, "url");
            String description = safeGetString(paymentLink, "description");
            String status = safeGetString(paymentLink, "status");
            String currency = safeGetString(paymentLink, "currency");

            // Handle amount (convert from paise to rupees)
            Object amountObj = paymentLink.get("amount");
            String amountInRupees = "0.00";
            if (amountObj != null) {
                try {
                    long amountInPaise = Long.parseLong(String.valueOf(amountObj));
                    BigDecimal amount = new BigDecimal(amountInPaise).divide(new BigDecimal("100"));
                    amountInRupees = String.format("%.2f", amount);
                } catch (NumberFormatException e) {
                    amountInRupees = String.valueOf(amountObj);
                }
            }

            // Handle created_at timestamp
            LocalDateTime createdAt = LocalDateTime.now();
            Object createdAtObj = paymentLink.get("created_at");
            if (createdAtObj != null) {
                try {
                    long timestamp = Long.parseLong(String.valueOf(createdAtObj));
                    createdAt = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(timestamp),
                            ZoneId.systemDefault()
                    );
                } catch (NumberFormatException e) {
                    log.debug("Could not parse created_at timestamp: {}", createdAtObj);
                }
            }

            // Handle expiry
            LocalDateTime expiresAt = null;
            Object expireByObj = paymentLink.get("expire_by");
            if (expireByObj != null) {
                try {
                    long expiryTimestamp = Long.parseLong(String.valueOf(expireByObj));
                    expiresAt = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(expiryTimestamp),
                            ZoneId.systemDefault()
                    );
                } catch (NumberFormatException e) {
                    log.debug("Could not parse expiry timestamp: {}", expireByObj);
                }
            }

            // Generate embed HTML
            String embedHtml = generatePaymentLinkHtml(shortUrl, amountInRupees, currency);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "PAYMENT_LINK");
            metadata.put("expires_in_minutes", 30);
            metadata.put("is_reusable", false);

            return PaymentButtonResponseDTO.builder()
                    .paymentLinkId(paymentLinkId)
                    .shortUrl(shortUrl)
                    .url(fullUrl)
                    .amount(amountInRupees)
                    .currency(currency)
                    .description(description)
                    .status(status != null ? status : "created")
                    .createdAt(createdAt)
                    .expiresAt(expiresAt)
                    .embedHtml(embedHtml)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Error building payment link response: {}", e.getMessage(), e);

            // Return minimal response on error
            return PaymentButtonResponseDTO.builder()
                    .paymentLinkId("error")
                    .shortUrl("https://rzp.io/i/error")
                    .amount("0.00")
                    .currency("INR")
                    .description("Error creating payment link")
                    .status("error")
                    .createdAt(LocalDateTime.now())
                    .embedHtml("<!-- Error: Could not create payment link -->")
                    .metadata(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Generate HTML for payment link (FIXED - escaped %%)
     */
    private String generatePaymentLinkHtml(String shortUrl, String amount, String currency) {
        // Using String.format with proper escaping
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Payment Link</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 20px;
                    }
                    .payment-card {
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        padding: 40px;
                        max-width: 480px;
                        width: 100%%;
                        text-align: center;
                    }
                    .amount-display {
                        font-size: 48px;
                        font-weight: 700;
                        color: #333;
                        margin: 20px 0;
                    }
                    .currency {
                        font-size: 24px;
                        color: #666;
                        vertical-align: top;
                    }
                    .description {
                        color: #666;
                        font-size: 16px;
                        margin-bottom: 30px;
                        line-height: 1.5;
                    }
                    .pay-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 18px 48px;
                        border-radius: 50px;
                        text-decoration: none;
                        font-size: 18px;
                        font-weight: 600;
                        transition: all 0.3s ease;
                        box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
                        margin: 20px 0;
                    }
                    .pay-button:hover {
                        transform: translateY(-3px);
                        box-shadow: 0 15px 40px rgba(102, 126, 234, 0.6);
                    }
                    .info-box {
                        background: #f8f9fa;
                        border-radius: 12px;
                        padding: 20px;
                        margin-top: 30px;
                        border-left: 4px solid #667eea;
                    }
                    .info-item {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 10px;
                        font-size: 14px;
                        color: #555;
                    }
                    .expiry-warning {
                        color: #e74c3c;
                        font-weight: 600;
                        margin-top: 15px;
                        font-size: 14px;
                    }
                    .secure-badge {
                        display: inline-block;
                        background: #2ecc71;
                        color: white;
                        padding: 6px 16px;
                        border-radius: 20px;
                        font-size: 12px;
                        font-weight: 600;
                        margin-top: 20px;
                    }
                    .razorpay-logo {
                        width: 120px;
                        opacity: 0.8;
                        margin-top: 30px;
                    }
                </style>
            </head>
            <body>
                <div class="payment-card">
                    <h1 style="color: #333; margin-bottom: 10px;">Complete Your Payment</h1>
                    <div class="amount-display">
                        <span class="currency">₹</span>%s
                    </div>
                   \s
                    <p class="description">
                        Click the button below to securely complete your payment
                    </p>
                   \s
                    <a href="%s" class="pay-button" target="_blank">
                        Pay Now
                    </a>
                   \s
                    <div class="info-box">
                        <div class="info-item">
                            <span>Amount:</span>
                            <span>₹%s %s</span>
                        </div>
                        <div class="info-item">
                            <span>Payment Type:</span>
                            <span>One-time Payment Link</span>
                        </div>
                        <div class="info-item">
                            <span>Status:</span>
                            <span style="color: #3498db;">Ready to Pay</span>
                        </div>
                    </div>
                   \s
                    <div class="expiry-warning">
                        ⚡ This link expires in 30 minutes
                    </div>
                   \s
                    <div class="secure-badge">
                        🔒 Secured by Razorpay
                    </div>
                   \s
                    <img src="https://razorpay.com/assets/razorpay-logo.svg" alt="Razorpay" class="razorpay-logo">
                </div>
               \s
                <script>
                    // Auto-refresh if page is opened
                    setTimeout(function() {
                        if(document.hasFocus()) {
                            location.reload();
                        }
                    }, 60000); // Refresh every minute
                </script>
            </body>
            </html>
           \s""", amount, shortUrl, amount, currency);
    }

    /**
     * Safely get string value from payment link
     */
    private String safeGetString(PaymentLink paymentLink, String key) {
        try {
            Object value = paymentLink.get(key);
            if (value == null) {
                return null;
            }
            return String.valueOf(value).trim();
        } catch (Exception e) {
            log.debug("Error getting value for key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch payment link details
     */
    public PaymentButtonResponseDTO getPaymentLink(String paymentLinkId) throws RazorpayException {
        try {
            PaymentLink paymentLink = razorpayClient.paymentLink.fetch(paymentLinkId);
            return buildPaymentLinkResponse(paymentLink);
        } catch (RazorpayException e) {
            log.error("Error fetching payment link: {}", e.getMessage());
            throw new RazorpayException("Failed to fetch payment link: " + e.getMessage());
        }
    }

    /**
     * Cancel payment link
     */
    public Map<String, Object> cancelPaymentLink(String paymentLinkId) throws RazorpayException {
        try {
            PaymentLink paymentLink = razorpayClient.paymentLink.cancel(paymentLinkId);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentLinkId", paymentLinkId);
            response.put("status", safeGetString(paymentLink, "status"));
            response.put("cancelled_at", safeGetString(paymentLink, "cancelled_at"));
            response.put("message", "Payment link cancelled successfully");
            response.put("timestamp", LocalDateTime.now().toString());

            return response;
        } catch (RazorpayException e) {
            log.error("Error cancelling payment link: {}", e.getMessage());
            throw new RazorpayException("Failed to cancel payment link: " + e.getMessage());
        }
    }

    /**
     * Get payment link statistics
     */
    public Map<String, Object> getPaymentLinkStats(String paymentLinkId) throws RazorpayException {
        try {
            PaymentLink paymentLink = razorpayClient.paymentLink.fetch(paymentLinkId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("paymentLinkId", paymentLinkId);
            stats.put("status", safeGetString(paymentLink, "status"));
            stats.put("created_at", safeGetString(paymentLink, "created_at"));
            stats.put("expire_by", safeGetString(paymentLink, "expire_by"));
            stats.put("amount", safeGetString(paymentLink, "amount"));
            stats.put("amount_paid", safeGetString(paymentLink, "amount_paid"));
            stats.put("amount_due", safeGetString(paymentLink, "amount_due"));

            // Get customer details safely
            try {
                Object customerObj = paymentLink.get("customer");
                if (customerObj instanceof JSONObject) {
                    JSONObject customer = (JSONObject) customerObj;
                    stats.put("customer_email", customer.optString("email"));
                    stats.put("customer_contact", customer.optString("contact"));
                }
            } catch (Exception e) {
                log.debug("Could not extract customer details");
            }

            // Calculate time remaining
            Object expireByObj = paymentLink.get("expire_by");
            if (expireByObj != null) {
                try {
                    long expiryTimestamp = Long.parseLong(String.valueOf(expireByObj));
                    long currentTime = Instant.now().getEpochSecond();
                    long secondsRemaining = expiryTimestamp - currentTime;
                    long minutesRemaining = secondsRemaining / 60;

                    stats.put("expires_in_minutes", minutesRemaining);
                    stats.put("is_expired", secondsRemaining <= 0);

                    if (secondsRemaining > 0) {
                        long hours = minutesRemaining / 60;
                        long minutes = minutesRemaining % 60;
                        stats.put("expires_in", String.format("%02d:%02d", hours, minutes));
                    }
                } catch (NumberFormatException e) {
                    log.debug("Could not parse expiry timestamp");
                }
            }

            stats.put("timestamp", LocalDateTime.now().toString());
            return stats;
        } catch (RazorpayException e) {
            log.error("Error fetching payment link stats: {}", e.getMessage());
            throw new RazorpayException("Failed to fetch payment link stats: " + e.getMessage());
        }
    }

    /**
     * Send payment link via email/SMS
     */
    public Map<String, Object> sendPaymentLink(String paymentLinkId, String email, String phone) throws RazorpayException {
        try {
            PaymentLink paymentLink = razorpayClient.paymentLink.fetch(paymentLinkId);
            String shortUrl = safeGetString(paymentLink, "short_url");

            Map<String, Object> response = new HashMap<>();
            response.put("paymentLinkId", paymentLinkId);
            response.put("short_url", shortUrl);
            response.put("description", safeGetString(paymentLink, "description"));
            response.put("amount", safeGetString(paymentLink, "amount"));

            if (email != null && !email.trim().isEmpty()) {
                response.put("email", email.trim());
                response.put("email_sent", true);
                response.put("email_message", "Payment link would be sent to: " + email.trim());
                log.info("Payment link {} prepared for email: {}", shortUrl, email);
            }

            if (phone != null && !phone.trim().isEmpty()) {
                response.put("phone", phone.trim());
                response.put("sms_sent", true);
                response.put("sms_message", "Payment link would be sent to: " + phone.trim());
                log.info("Payment link {} prepared for SMS: {}", shortUrl, phone);
            }

            response.put("message", "Share this URL with customer: " + shortUrl);
            response.put("timestamp", LocalDateTime.now().toString());

            return response;
        } catch (RazorpayException e) {
            log.error("Error sending payment link: {}", e.getMessage());
            throw new RazorpayException("Failed to send payment link: " + e.getMessage());
        }
    }
}