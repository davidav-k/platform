package com.example.user_service.utils;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailUtils {

    private static String baseUrl;

    @Value("${api.endpoint.base-url}")
    public void setBaseUrl(String baseUrl){
        EmailUtils.baseUrl = baseUrl;
    }

    public static String getEmailMessage(String name, String host, String key) {
        return "Hello " + name + ",\n\nYour new account has been created. Please click on the link below to verify your account.\n\n" +
                getVerificationUrl(host, key) + "\n\n The Support Team";
    }

    public static String getResetPasswordMessage(String name, String host, String token) {
        return "Hello " + name + ",\n\nYour account has been change password. Please click on the link below to verify change password.\n\n" +
                getResetPasswordUrl(host, token) + "\n\n The Support Team";
    }

    private static String getVerificationUrl(String host, String key) {
        return host + baseUrl + "/user/verify/account?key=" + key;
    }

    private static String getResetPasswordUrl(String host, String token) {
        return host + baseUrl + "/user/verify/password?key=" + token;
    }
}


