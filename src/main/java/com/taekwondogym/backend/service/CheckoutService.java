package com.taekwondogym.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.taekwondogym.backend.model.CartItem;
import com.taekwondogym.backend.model.Order;
import com.taekwondogym.backend.model.User;
import com.taekwondogym.backend.repository.OrderRepository;
import com.taekwondogym.backend.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private static final String RECEIPT_UPLOAD_DIR = "uploads/receipts/";

    public void processCheckout(String bankId, MultipartFile receipt) throws IOException {
        String email = getCurrentUserEmail();
        if (email == null) {
            throw new IllegalStateException("Checkout requires login.");
        }

        // Get cart items
        List<CartItem> cartItems = cartService.getCartItems(email, null);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }

        // Calculate total amount
        double totalAmount = calculateTotal(cartItems);

        // Handle receipt upload
        String receiptFilePath = saveReceipt(receipt);

        // Create order
        Order order = new Order();
        order.setTotalAmount(totalAmount);
        order.setEmail(email);
        order.setBankId(bankId);
        order.setReceiptFilePath(receiptFilePath);
        orderRepository.save(order);

        // Clear the cart after checkout
        cartService.clearCart(email, null);
    }

    private double calculateTotal(List<CartItem> items) {
        return items.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }

    private String saveReceipt(MultipartFile receipt) throws IOException {
        if (receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt file is required.");
        }

        File directory = new File(RECEIPT_UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs(); // Create directory if not exists
        }

        String fileName = UUID.randomUUID().toString() + "_" + receipt.getOriginalFilename();
        File file = new File(RECEIPT_UPLOAD_DIR + fileName);
        receipt.transferTo(file);

        return file.getAbsolutePath();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }
}
