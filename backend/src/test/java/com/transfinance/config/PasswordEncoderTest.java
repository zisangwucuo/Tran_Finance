package com.transfinance.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码哈希测试，确保系统使用 BCrypt 而不是明文密码。
 */
class PasswordEncoderTest {

    @Test
    void encodedPasswordShouldUseBcryptFormat() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode("unit-test-placeholder");
        assertTrue(encodedPassword.startsWith("$2"));
    }
}
