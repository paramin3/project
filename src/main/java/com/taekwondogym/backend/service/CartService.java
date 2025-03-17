package com.taekwondogym.backend.service;

import com.taekwondogym.backend.model.Cart;
import com.taekwondogym.backend.model.CartItem;
import com.taekwondogym.backend.model.Product;
import com.taekwondogym.backend.model.User;
import com.taekwondogym.backend.repository.CartRepository;
import com.taekwondogym.backend.repository.ProductRepository;
import com.taekwondogym.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public Cart getOrCreateCart(String email, String sessionId) {
        if (email != null) {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                System.out.println("User not found: " + email);
                return null;
            }

            Optional<Cart> userCartOpt = cartRepository.findByUser(user);

            if (sessionId != null) {
                Optional<Cart> guestCartOpt = cartRepository.findBySessionId(sessionId);
                if (guestCartOpt.isPresent()) {
                    System.out.println("Guest cart found for session ID: " + sessionId);
                    Cart guestCart = guestCartOpt.get();
                    Cart userCart = userCartOpt.orElseGet(() -> createCartForUser(user));

                    mergeCarts(userCart, guestCart);
                    cartRepository.delete(guestCart);
                    System.out.println("Merged guest cart into user cart: " + email);
                    return cartRepository.save(userCart);
                }
            }

            System.out.println("User cart retrieved for: " + email);
            return userCartOpt.orElseGet(() -> createCartForUser(user));
        } 
        else if (sessionId != null) {
            Optional<Cart> guestCartOpt = cartRepository.findBySessionId(sessionId);
            if (guestCartOpt.isPresent()) {
                System.out.println("Guest cart retrieved for session ID: " + sessionId);
                return guestCartOpt.get();
            } else {
                return createCartForSession(sessionId);
            }
        }

        System.out.println("Cart retrieval failed - Email: " + email + ", SessionId: " + sessionId);
        return null;
    }


    private void mergeCarts(Cart userCart, Cart guestCart) {
        for (CartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existingItemOpt = userCart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(guestItem.getProduct().getId()))
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                // If product already exists in user cart, update quantity
                CartItem existingItem = existingItemOpt.get();
                existingItem.setQuantity(existingItem.getQuantity() + guestItem.getQuantity());
            } else {
                // If not, move item to user cart
                guestItem.setCart(userCart);
                userCart.getItems().add(guestItem);
            }
        }
    }

    private Cart createCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private Cart createCartForSession(String sessionId) {
        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        Cart savedCart = cartRepository.save(cart);
        
        System.out.println("Guest cart created with Session ID: " + sessionId);
        return savedCart;
    }

    // เพิ่มสินค้าไปยังตะกร้า
    public Cart addProductToCart(String email, String sessionId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(email, sessionId);
        if (cart == null) {
            return null;
        }

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();

            if (!product.isAvailable()) {
                return null; // Do not add unavailable products
            }

            if (product.getStock() < quantity) {
                return null; // Insufficient stock
            }

            CartItem existingItem = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity); // Update quantity
            } else {
                CartItem newItem = new CartItem();
                newItem.setProduct(product);
                newItem.setQuantity(quantity);
                newItem.setCart(cart);
                cart.getItems().add(newItem);
            }
            return cartRepository.save(cart);
        }
        return null; // Product not found
    }

    // ลบสินค้าออกจากตะกร้า
    public void removeProductFromCart(String email, String sessionId, Long productId) {
        Cart cart = getOrCreateCart(email, sessionId);
        if (cart != null) {
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
            cartRepository.save(cart);
        }
    }

    // ดึงรายการสินค้าในตะกร้า (แก้จาก getCartItems เป็น getItems)
    public List<CartItem> getCartItems(String email, String sessionId) {
        Cart cart = getOrCreateCart(email, sessionId);
        if (cart == null) {
            System.out.println("Cart not found - Email: " + email + ", SessionId: " + sessionId);
            return new ArrayList<>(); // คืนลิสต์ว่างถ้าไม่พบตะกร้า
        }
        List<CartItem> items = cart.getItems(); // เปลี่ยนจาก getCartItems เป็น getItems
        System.out.println("Cart items retrieved - Email: " + email + ", SessionId: " + sessionId + ", Items count: " + items.size());
        return items;
    }

    // อัปเดตจำนวนสินค้าในตะกร้า
    public void updateQuantityInCart(String email, String sessionId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(email, sessionId);
        if (cart != null) {
            Optional<CartItem> itemOptional = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .findFirst();

            if (itemOptional.isPresent()) {
                CartItem item = itemOptional.get();
                item.setQuantity(quantity);
                cartRepository.save(cart);
            }
        }
    }

    // ล้างตะกร้าทั้งหมด
    public void clearCart(String email, String sessionId) {
        Cart cart = getOrCreateCart(email, sessionId);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    // ลบสินค้าที่ไม่พร้อมขายออกจากตะกร้า
    public void removeUnavailableProductsFromCart() {
        List<Cart> carts = cartRepository.findAll();
        for (Cart cart : carts) {
            List<CartItem> itemsToRemove = cart.getItems().stream()
                    .filter(item -> !item.getProduct().isAvailable())
                    .collect(Collectors.toList());

            cart.getItems().removeAll(itemsToRemove);
            cartRepository.save(cart);
        }
    }

    // ตรวจสอบการชำระเงิน (ต้องล็อกอิน)
    public String checkout(String email) {
        if (email == null) {
            return "You must be logged in to proceed with checkout.";
        }

        Cart cart = getOrCreateCart(email, null);
        if (cart == null || cart.getItems().isEmpty()) {
            return "Cart is empty. Cannot proceed with checkout.";
        }

        // ดำเนินการเช็คเอาท์
        cart.getItems().clear();
        cartRepository.save(cart);
        return "Checkout successful.";
    }
}