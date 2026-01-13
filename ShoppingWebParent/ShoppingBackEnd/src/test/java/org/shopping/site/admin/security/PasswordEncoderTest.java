package org.shopping.site.admin.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderTest {
    public static void main(String[] args) {
        String password = "password"; // ← change this if needed
        String encoded = new BCryptPasswordEncoder().encode(password);
        System.out.println("Encoded password: " + encoded);
    }
}
