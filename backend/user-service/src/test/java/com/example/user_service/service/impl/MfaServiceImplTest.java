package com.example.user_service.service.impl;

import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class MfaServiceImplTest {

    private MfaServiceImpl mfaService;

    @BeforeEach
    void setUp() {
        mfaService = new MfaServiceImpl();
    }

    @Test
    void generatedSecretKeyShouldNotBeEmpty() {
        String secretKey = mfaService.generateSecretKey();

        assertNotNull(secretKey);
        assertFalse(secretKey.isEmpty());
    }

    @Test
    void generatedSecretKeysShouldBeDifferent() {
        String secretKey1 = mfaService.generateSecretKey();
        String secretKey2 = mfaService.generateSecretKey();

        assertNotEquals(secretKey1, secretKey2);
    }

    @Test
    void generateQrCodeUrlShouldContainEmailAndSecretKey() {
        String email = "test@example.com";
        String secretKey = "ABCDEFGHIJKLMNOP";

        String qrCodeUrl = mfaService.generateQrCodeUrl(email, secretKey);

        assertNotNull(qrCodeUrl);
        assertTrue(qrCodeUrl.startsWith("https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl="));

        String encodedPart = qrCodeUrl.substring(qrCodeUrl.indexOf("chl=") + 4);
        String decodedUrl = new String(Base64.getUrlDecoder().decode(encodedPart));

        assertTrue(decodedUrl.contains(email));
        assertTrue(decodedUrl.contains(secretKey));
    }

    @Test
    void validateOtpShouldReturnFalseForInvalidCode() {
        String secretKey = mfaService.generateSecretKey();
        int invalidCode = 123456; // Assuming this is invalid for the current time

        boolean result = mfaService.validateOtp(secretKey, invalidCode);

        assertFalse(result);
    }

    @Test
    void validateOtpShouldReturnTrueForValidCode() {
        String secretKey = mfaService.generateSecretKey();
        Totp totp = new Totp(secretKey);
        int validCode = Integer.parseInt(totp.now());

        boolean result = mfaService.validateOtp(secretKey, validCode);

        assertTrue(result);
    }
}