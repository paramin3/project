package com.taekwondogym.backend.controller;

import com.taekwondogym.backend.dto.CartItemDTO;
import com.taekwondogym.backend.model.Cart;
import com.taekwondogym.backend.model.CartItem;
import com.taekwondogym.backend.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString(); // Generate a new session ID
            session.setAttribute("sessionId", sessionId);
        }
        return sessionId;
    }

    // Retrieve cart
    @GetMapping
    public ResponseEntity<Cart> getCart() {
        String email = getCurrentUserEmail();
        Cart cart;

        if (email != null) {
            cart = cartService.getOrCreateCart(email, null);
            session.removeAttribute("sessionId"); // Remove session ID when user logs in
        } else {
            String sessionId = getSessionId();
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
