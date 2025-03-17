package com.taekwondogym.backend.controller;

import com.taekwondogym.backend.dto.CartItemDTO;
import com.taekwondogym.backend.model.Cart;
import com.taekwondogym.backend.model.CartItem;
import com.taekwondogym.backend.service.CartService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private HttpSession session; // Used to manage sessionId

    // Retrieve current logged-in user's email
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    // Retrieve or generate session ID for guests
    private String getSessionId() {
        String sessionId = (String) session.getAttribute("sessionId");
        System.out.println("[CartController] Retrieved Session ID from session: " + sessionId);

        if (sessionId == null) {
            System.out.println("[CartController] Session ID is null, checking cookies...");
            Cookie[] cookies = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .getRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("sessionId".equals(cookie.getName())) {
                        sessionId = cookie.getValue();
                        System.out.println("[CartController] Retrieved Session ID from cookie: " + sessionId);
                        break;
                    }
                }
            }

            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                session.setAttribute("sessionId", sessionId);
                System.out.println("[CartController] New Session ID generated: " + sessionId);

                // Store in cookie
                Cookie cookie = new Cookie("sessionId", sessionId);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24); // 1 day
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getResponse().addCookie(cookie);
                System.out.println("[CartController] Stored new Session ID in cookie: " + sessionId);
            }
        }

        return sessionId;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart() {
        String email = getCurrentUserEmail();
        Cart cart;
        
        // Get sessionId from both session and cookie
        String sessionId = getSessionId();
        System.out.println("[CartController] Working with sessionId: " + sessionId);
        
        if (email != null) {
            // For logged-in users, pass the sessionId to allow merging
            cart = cartService.getOrCreateCart(email, sessionId);
            
            // Only after successful merging, remove the sessionId from both session and cookie
            if (sessionId != null) {
                session.removeAttribute("sessionId");
                
                // Add a new cookie with max-age=0 to remove it
                Cookie cookie = new Cookie("sessionId", "");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getResponse().addCookie(cookie);
                
                System.out.println("[CartController] Removed sessionId after cart merge");
            }
        } else {
            // For guests, use the sessionId
            cart = cartService.getOrCreateCart(null, sessionId);
        }
        
        return ResponseEntity.ok(cart);
    }

    // Add product to cart
    @PostMapping("/products/{productId}")
    public ResponseEntity<?> addProductToCart(@PathVariable Long productId, @RequestParam int quantity) {
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body("Quantity must be greater than zero.");
        }

        String email = getCurrentUserEmail();
        String sessionId = (email == null) ? getSessionId() : null;

        Cart updatedCart = cartService.addProductToCart(email, sessionId, productId, quantity);

        if (updatedCart != null) {
            return ResponseEntity.ok(updatedCart);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found or unavailable.");
        }
    }

    // Remove product from cart
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<?> removeProductFromCart(@PathVariable Long productId) {
        String email = getCurrentUserEmail();
        String sessionId = (email == null) ? getSessionId() : null;

        cartService.removeProductFromCart(email, sessionId, productId);
        return ResponseEntity.ok("Product removed from cart.");
    }

    // Retrieve all cart items
    @GetMapping("/products")
    public ResponseEntity<List<CartItem>> getProductsInCart() {
        String email = getCurrentUserEmail();
        String sessionId = (email == null) ? getSessionId() : null;
        System.out.println("Retrieving cart items - Email: " + email + ", Session ID: " + sessionId);

        List<CartItem> cartItems = cartService.getCartItems(email, sessionId);
        return ResponseEntity.ok(cartItems);
    }


    // Update quantity in cart
    @PutMapping("/products/{productId}")
    public ResponseEntity<?> updateProductQuantity(@PathVariable Long productId, @RequestBody CartItemDTO cartItemDTO) {
        if (cartItemDTO.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body("Quantity must be greater than zero.");
        }

        String email = getCurrentUserEmail();
        String sessionId = (email == null) ? getSessionId() : null;

        cartService.updateQuantityInCart(email, sessionId, productId, cartItemDTO.getQuantity());
        return ResponseEntity.ok("Product quantity updated.");
    }

    // Checkout (only allowed when logged in)
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout() {
        String email = getCurrentUserEmail();
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in to proceed with checkout.");
        }
        cartService.checkout(email);
        return ResponseEntity.ok("Checkout successful.");
    }
}
