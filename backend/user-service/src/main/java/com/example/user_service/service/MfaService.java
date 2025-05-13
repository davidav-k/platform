package com.example.user_service.service;

public interface MfaService {
    String generateSecretKey();

    String generateQrCodeUrl(String email, String secretKey);

    boolean validateOtp(String secret, int code);
}
