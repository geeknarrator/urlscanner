package com.geeknarrator.urlscanner.controller;

import com.geeknarrator.urlscanner.entity.User;
import com.geeknarrator.urlscanner.repository.UserRepository;
import com.geeknarrator.urlscanner.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email is already in use!"));
        }
        
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName()
        );
        
        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser.getEmail());
        
        return ResponseEntity.ok(new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), 
                savedUser.getFirstName(), savedUser.getLastName()));
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            User user = (User) authentication.getPrincipal();
            String token = jwtUtil.generateToken(user.getEmail());
            
            return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail(), 
                    user.getFirstName(), user.getLastName()));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Invalid email or password!"));
        }
    }
    
    public static class LoginRequest {
        @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.NotBlank
        private String email;
        
        @jakarta.validation.constraints.NotBlank
        private String password;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    public static class RegisterRequest {
        @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.NotBlank
        private String email;
        
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 6)
        private String password;
        
        @jakarta.validation.constraints.NotBlank
        private String firstName;
        
        @jakarta.validation.constraints.NotBlank
        private String lastName;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
    
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String email;
        private String firstName;
        private String lastName;
        
        public AuthResponse(String token, Long userId, String email, String firstName, String lastName) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }
        
        public String getToken() {
            return token;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
    }
    
    public static class ApiResponse {
        private Boolean success;
        private String message;
        
        public ApiResponse(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public Boolean getSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}