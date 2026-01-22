package com.example.walletmicroservice.service;

import com.example.walletmicroservice.dto.RazorpayQRCodeRequestDTO;
import com.example.walletmicroservice.dto.RazorpayQRCodeResponseDTO;
import com.example.walletmicroservice.entity.PaymentTransaction;
import com.example.walletmicroservice.repository.PaymentTransactionRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayQRCodeService {

    private final RazorpayClient razorpayClient;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * Create Razorpay QR Code for UPI payment
     * Using direct HTTP call as Razorpay Java SDK might not have QR code support
     */
    @Transactional
    public RazorpayQRCodeResponseDTO createQRCode(RazorpayQRCodeRequestDTO request) throws RazorpayException, IOException {
        // 1. Generate internal transaction ID
        String internalTransactionId = "QR" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        // 2. Create Razorpay Order first (optional, but good for tracking)
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", request.getOrderId());
        orderRequest.put("payment_capture", 1);

        JSONObject orderNotes = new JSONObject();
        orderNotes.put("customer_id", request.getCustomerId());
        orderNotes.put("order_id", request.getOrderId());
        orderNotes.put("description", request.getDescription());
        orderNotes.put("transaction_id", internalTransactionId);
        orderRequest.put("notes", orderNotes);

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        // 3. Create QR Code using Razorpay API
        JSONObject qrCodeRequest = new JSONObject();
        qrCodeRequest.put("type", "upi_qr");
        qrCodeRequest.put("name", "Order: " + request.getOrderId());
        qrCodeRequest.put("usage", "single_use");
        qrCodeRequest.put("fixed_amount", true);
        qrCodeRequest.put("payment_amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        qrCodeRequest.put("description", request.getDescription() != null ? request.getDescription() : "Payment for order: " + request.getOrderId());
        qrCodeRequest.put("customer_id", request.getCustomerId());

        // Set QR code expiry (30 minutes from now)
        long closeBy = Instant.now().getEpochSecond() + (30 * 60);
        qrCodeRequest.put("close_by", closeBy);

        JSONObject qrNotes = new JSONObject();
        qrNotes.put("order_id", request.getOrderId());
        qrNotes.put("razorpay_order_id", razorpayOrderId);
        qrNotes.put("customer_id", request.getCustomerId());
        qrNotes.put("transaction_id", internalTransactionId);
        qrCodeRequest.put("notes", qrNotes);

        // Make HTTP request to Razorpay API
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                qrCodeRequest.toString()
        );

        String credentials = razorpayKeyId + ":" + razorpayKeySecret;
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        Request httpRequest = new Request.Builder()
                .url("https://api.razorpay.com/v1/payments/qr_codes")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Basic " + base64Credentials)
                .build();

        Response response = httpClient.newCall(httpRequest).execute();
        String responseBody = response.body().string();

        if (!response.isSuccessful()) {
            log.error("Failed to create QR code. Response: {}", responseBody);
            throw new RuntimeException("Failed to create QR code: " + responseBody);
        }

        JSONObject qrCodeResponse = new JSONObject(responseBody);
        String razorpayQRCodeId = qrCodeResponse.getString("id");

        // 4. Save to payment_transactions table
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(internalTransactionId);
        transaction.setRazorpayOrderId(razorpayOrderId);
        transaction.setCustomerId(request.getCustomerId());
        transaction.setOrderId(request.getOrderId());
        transaction.setTransactionType(PaymentTransaction.TransactionType.PAYIN);
        transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.UPI);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setMetadata(new JSONObject()
                .put("razorpay_order_response", razorpayOrder.toString())
                .put("razorpay_qrcode_response", qrCodeResponse.toString())
                .put("qr_code_id", razorpayQRCodeId)
                .put("description", request.getDescription())
                .put("close_by", closeBy)
                .toString());

        paymentTransactionRepository.save(transaction);

        // 5. Prepare response
        RazorpayQRCodeResponseDTO qrResponse = new RazorpayQRCodeResponseDTO();
        qrResponse.setTransactionId(internalTransactionId);
        qrResponse.setRazorpayOrderId(razorpayOrderId);
        qrResponse.setRazorpayQRCodeId(razorpayQRCodeId);
        qrResponse.setQrCodeImageUrl(qrCodeResponse.optString("image_url"));
        qrResponse.setQrShortUrl(qrCodeResponse.optString("short_url"));

        // Download QR code image as base64
        String imageUrl = qrCodeResponse.optString("image_url");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Request imageRequest = new Request.Builder()
                        .url(imageUrl)
                        .addHeader("Authorization", "Basic " + base64Credentials)
                        .build();

                Response imageResponse = httpClient.newCall(imageRequest).execute();
                if (imageResponse.isSuccessful() && imageResponse.body() != null) {
                    byte[] imageBytes = imageResponse.body().bytes();
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    qrResponse.setQrCodeImageBase64(base64Image);
                }
            } catch (Exception e) {
                log.warn("Failed to download QR code image: {}", e.getMessage());
            }
        }

        // Generate UPI links
        qrResponse.setUpiLink(generateUPILink(qrCodeResponse));
        qrResponse.setPaymentUrl("https://pay.razorpay.com/" + razorpayOrderId);
        qrResponse.setAmount(request.getAmount());
        qrResponse.setCurrency(request.getCurrency());
        qrResponse.setCustomerId(request.getCustomerId());
        qrResponse.setOrderId(request.getOrderId());
        qrResponse.setStatus("qr_code_created");
        qrResponse.setQrStatus(qrCodeResponse.optString("status", "active"));
        qrResponse.setCreatedAt(System.currentTimeMillis() / 1000);

        log.info("Razorpay QR Code created successfully. QR ID: {}, Order: {}", razorpayQRCodeId, request.getOrderId());

        return qrResponse;
    }

    /**
     * Generate UPI link from QR code response
     */
    private String generateUPILink(JSONObject qrCodeResponse) {
        // Try to get UPI link from response
        String upiLink = qrCodeResponse.optString("upi_link");
        if (upiLink != null && !upiLink.isEmpty()) {
            return upiLink;
        }

        // Fallback: Try to get from qr_string
        String qrString = qrCodeResponse.optString("qr_string");
        if (qrString != null && !qrString.isEmpty()) {
            // QR string is the UPI payment URI
            return qrString;
        }

        // Fallback: Generate from short URL
        String shortUrl = qrCodeResponse.optString("short_url");
        if (shortUrl != null && !shortUrl.isEmpty()) {
            return "upi://pay?pa=" + extractUpiIdFromShortUrl(shortUrl);
        }

        return null;
    }

    /**
     * Extract UPI ID from short URL (simplified)
     */
    private String extractUpiIdFromShortUrl(String shortUrl) {
        // Short URL format: https://rzp.io/i/{code}
        // In real implementation, you might need to resolve the short URL
        return "qr@razorpay"; // Placeholder
    }

    /**
     * Get QR Code Status
     */
    public RazorpayQRCodeResponseDTO getQRCodeStatus(String transactionId) throws IOException {
        PaymentTransaction transaction = paymentTransactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        JSONObject metadata = new JSONObject(transaction.getMetadata());
        String razorpayQRCodeId = metadata.optString("qr_code_id");

        if (razorpayQRCodeId == null || razorpayQRCodeId.isEmpty()) {
            throw new RuntimeException("No Razorpay QR Code ID found for transaction");
        }

        // Fetch QR code details from Razorpay API
        String credentials = razorpayKeyId + ":" + razorpayKeySecret;
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url("https://api.razorpay.com/v1/payments/qr_codes/" + razorpayQRCodeId)
                .get()
                .addHeader("Authorization", "Basic " + base64Credentials)
                .build();

        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to fetch QR code: " + responseBody);
        }

        JSONObject qrCodeResponse = new JSONObject(responseBody);

        RazorpayQRCodeResponseDTO qrStatus = new RazorpayQRCodeResponseDTO();
        qrStatus.setTransactionId(transaction.getTransactionId());
        qrStatus.setRazorpayOrderId(transaction.getRazorpayOrderId());
        qrStatus.setRazorpayQRCodeId(razorpayQRCodeId);
        qrStatus.setQrCodeImageUrl(qrCodeResponse.optString("image_url"));
        qrStatus.setQrShortUrl(qrCodeResponse.optString("short_url"));
        qrStatus.setUpiLink(qrCodeResponse.optString("upi_link"));
        qrStatus.setAmount(transaction.getAmount());
        qrStatus.setCurrency(transaction.getCurrency());
        qrStatus.setCustomerId(transaction.getCustomerId());
        qrStatus.setOrderId(transaction.getOrderId());
        qrStatus.setStatus(transaction.getStatus().toString());
        qrStatus.setQrStatus(qrCodeResponse.optString("status"));
        qrStatus.setCreatedAt(transaction.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC));

        return qrStatus;
    }

    /**
     * Close QR Code
     */
    public void closeQRCode(String transactionId) throws IOException {
        PaymentTransaction transaction = paymentTransactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        JSONObject metadata = new JSONObject(transaction.getMetadata());
        String razorpayQRCodeId = metadata.optString("qr_code_id");

        if (razorpayQRCodeId == null || razorpayQRCodeId.isEmpty()) {
            throw new RuntimeException("No Razorpay QR Code ID found");
        }

        // Close QR code
        JSONObject closeRequest = new JSONObject();
        closeRequest.put("status", "closed");

        String credentials = razorpayKeyId + ":" + razorpayKeySecret;
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                closeRequest.toString()
        );

        Request request = new Request.Builder()
                .url("https://api.razorpay.com/v1/payments/qr_codes/" + razorpayQRCodeId + "/close")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Basic " + base64Credentials)
                .build();

        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to close QR code: " + response.body().string());
        }

        log.info("QR Code closed: {}", razorpayQRCodeId);
    }

    /**
     * Fetch payments for a QR code
     */
    public JSONObject getQRCodePayments(String transactionId) throws IOException {
        PaymentTransaction transaction = paymentTransactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        JSONObject metadata = new JSONObject(transaction.getMetadata());
        String razorpayQRCodeId = metadata.optString("qr_code_id");

        if (razorpayQRCodeId == null || razorpayQRCodeId.isEmpty()) {
            throw new RuntimeException("No Razorpay QR Code ID found");
        }

        String credentials = razorpayKeyId + ":" + razorpayKeySecret;
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url("https://api.razorpay.com/v1/payments/qr_codes/" + razorpayQRCodeId + "/payments")
                .get()
                .addHeader("Authorization", "Basic " + base64Credentials)
                .build();

        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to fetch QR code payments: " + responseBody);
        }

        return new JSONObject(responseBody);
    }

    /**
     * Check if QR code payment is successful
     */
    public boolean isQRCodePaymentSuccessful(String transactionId) throws IOException {
        JSONObject payments = getQRCodePayments(transactionId);

        if (payments.has("items")) {
            org.json.JSONArray items = payments.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject payment = items.getJSONObject(i);
                if ("captured".equals(payment.optString("status"))) {
                    return true;
                }
            }
        }

        return false;
    }
}