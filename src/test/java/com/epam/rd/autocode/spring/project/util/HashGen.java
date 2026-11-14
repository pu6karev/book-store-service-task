package com.epam.rd.autocode.spring.project.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// class for generate a password that will be added to all demo users sql.sql
public class HashGen {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("password123"));
    }
}
