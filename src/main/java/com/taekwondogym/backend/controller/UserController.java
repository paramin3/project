package com.taekwondogym.backend.controller;

import com.taekwondogym.backend.dto.LoginRequest;
import com.taekwondogym.backend.model.User;
import com.taekwondogym.backend.service.CartService;
import com.taekwondogym.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private CartService cartService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(Map.of("error", e.getMessage())); // ส่งข้อความ error กลับไป
        }
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // Extract sessionId from cookie before authentication
            String guestSessionId = null;
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("sessionId".equals(cookie.getName())) {
                        guestSessionId = cookie.getValue();
                        System.out.println("[UserController] Found sessionId in cookie: " + guestSessionId);
                        break;
                    }
                }
            }
            
            // Standard authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                                 SecurityContextHolder.getContext());
            
            // Merge carts immediately after successful authentication
            if (guestSessionId != null) {
                System.out.println("[UserController] Merging cart with sessionId: " + guestSessionId);
                cartService.getOrCreateCart(loginRequest.getEmail(), guestSessionId);
                
                // Clear sessionId cookie after merging
                Cookie cookie = new Cookie("sessionId", "");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getResponse().addCookie(cookie);
            }

            return ResponseEntity.ok(new LoginResponse("Login successful", loginRequest.getEmail()));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(new LoginResponse("Invalid email or password", null));
        } catch (Exception ex) {
            System.out.println("[UserController] Login exception: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new LoginResponse("An error occurred during login", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new LoginResponse("Logout successful", null));
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> currentUser(Principal principal) {
        if (principal != null) {
            return ResponseEntity.ok(Map.of("email", principal.getName())); // ส่งในรูปแบบ JSON
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
    }
}

class LoginResponse {
    private String message;
    private String email; // Changed from username to email

    public LoginResponse(String message, String email) { // Constructor updated to email
        this.message = message;
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() { // Changed from getUsername to getEmail
        return email;
    }

    public void setEmail(String email) { // Changed from setUsername to setEmail
        this.email = email;
    }
}
