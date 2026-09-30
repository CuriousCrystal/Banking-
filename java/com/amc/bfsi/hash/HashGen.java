package com.amc.bfsi.hash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();   // cost 10 by default
        String hash = enc.encode("admin123");
        System.out.println(hash);
        System.out.println(enc.matches("admin123", hash));        // true
    }
}