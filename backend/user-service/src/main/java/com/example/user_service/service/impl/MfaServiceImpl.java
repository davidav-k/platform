package com.example.user_service.service.impl;

import com.example.user_service.service.MfaService;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class MfaServiceImpl implements MfaService {

    private static final String QR_CODE_URL = "https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl=";

    @Override
    public String generateSecretKey() {
        return Base32.random();
    }

    @Override
    public String generateQrCodeUrl(String email, String secretKey) {
        String otpAuthUrl = "otpauth://totp/UserService:" + email + "?secret=" + secretKey + "&issuer=UserService";
        return QR_CODE_URL + Base64.getUrlEncoder().encodeToString(otpAuthUrl.getBytes());
    }

    @Override
    public boolean validateOtp(String secret, int code) {
        Totp totp = new Totp(secret);
        return totp.verify(String.valueOf(code));
    }
}
