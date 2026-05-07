package com.khoj.lms.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Cryptography {

    private Cryptography() {}

    public static String hashOtp(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(otp.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("OTP hashing failed", e);
        }
    }
}