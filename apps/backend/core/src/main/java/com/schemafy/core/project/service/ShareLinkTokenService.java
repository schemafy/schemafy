package com.schemafy.core.project.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ShareLinkTokenService {

    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String pepper;

    public ShareLinkTokenService(
            @Value("${sharelink.pepper:${SHARELINK_PEPPER:default-pepper-change-me}}") String pepper) {
        this.pepper = pepper;
        if ("default-pepper-change-me".equals(pepper)) {
            log.warn(
                    "Using default pepper for ShareLink token hashing. Set SHARELINK_PEPPER environment variable in production!");
        }
    }

    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tokenBytes);
    }

    public byte[] hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = token + pepper;
            return digest.digest(salted.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean verifyToken(String token, byte[] storedHash) {
        byte[] computedHash = hashToken(token);
        return MessageDigest.isEqual(computedHash, storedHash);
    }

}
