package com.manhduc205.ezgear;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;

@Component
public class TestDB {
    @Value("${CLOUDINARY_CLOUD_NAME}")
    private static String cloudName;
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("123456"));
        System.out.println("Cloudinary Cloud Name = " + cloudName);
        byte[] key = new byte[64]; // 512-bit cho HS512
        new SecureRandom().nextBytes(key);
        String base64Key = Base64.getEncoder().encodeToString(key);
        System.out.println("Your JWT Secret Key (Base64): " + base64Key);
    }


}
