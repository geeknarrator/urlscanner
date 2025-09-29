package com.geeknarrator.urlscanner.security;

import com.geeknarrator.urlscanner.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {
    
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        
        throw new RuntimeException("Invalid user type in security context");
    }
    
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }
}