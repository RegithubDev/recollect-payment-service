package com.example.walletmicroservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayHttpClientService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.base-url}")
    private String razorpayBaseUrl;

    private final RestTemplate restTemplate;

    private HttpHeaders createHeaders() {
        String auth = razorpayKeyId + ":" + razorpayKeySecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // 1. Create Contact
    public JSONObject createContact(JSONObject contactRequest) {
        String url = razorpayBaseUrl + "/contacts";
        HttpEntity<String> entity = new HttpEntity<>(contactRequest.toString(), createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return new JSONObject(response.getBody());
        } catch (Exception e) {
            log.error("Error creating contact", e);
            throw new RuntimeException("Failed to create contact: " + e.getMessage());
        }
    }

    // 2. Create Fund Account
    public JSONObject createFundAccount(JSONObject fundAccountRequest) {
        String url = razorpayBaseUrl + "/fund_accounts";
        HttpEntity<String> entity = new HttpEntity<>(fundAccountRequest.toString(), createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return new JSONObject(response.getBody());
        } catch (Exception e) {
            log.error("Error creating fund account", e);
            throw new RuntimeException("Failed to create fund account: " + e.getMessage());
        }
    }

    // 3. Create Payout
    public JSONObject createPayout(JSONObject payoutRequest) {
        String url = razorpayBaseUrl + "/payouts";
        HttpEntity<String> entity = new HttpEntity<>(payoutRequest.toString(), createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return new JSONObject(response.getBody());
        } catch (Exception e) {
            log.error("Error creating payout", e);
            throw new RuntimeException("Failed to create payout: " + e.getMessage());
        }
    }

    // 4. Get Payout Status
    public JSONObject getPayoutStatus(String razorpayPayoutId) {
        String url = razorpayBaseUrl + "/payouts/" + razorpayPayoutId;
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return new JSONObject(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching payout status", e);
            throw new RuntimeException("Failed to fetch payout status: " + e.getMessage());
        }
    }

    // 5. Validate VPA
    public JSONObject validateVPA(String vpa) {
        String url = razorpayBaseUrl + "/fund_accounts/validity";

        JSONObject request = new JSONObject();
        request.put("vpa", vpa);

        HttpEntity<String> entity = new HttpEntity<>(request.toString(), createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return new JSONObject(response.getBody());
        } catch (Exception e) {
            log.error("Error validating VPA", e);
            throw new RuntimeException("Failed to validate VPA: " + e.getMessage());
        }
    }
}