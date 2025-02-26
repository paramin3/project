package com.taekwondogym.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.taekwondogym.backend.model.CartItem;
import com.taekwondogym.backend.model.Order;
import com.taekwondogym.backend.model.OrderItem;
import com.taekwondogym.backend.model.Product;
import com.taekwondogym.backend.model.User;
import com.taekwondogym.backend.repository.OrderRepository;
import com.taekwondogym.backend.repository.ProductRepository;
import com.taekwondogym.backend.repository.UserRepository;
import com.taekwondogym.backend.store.ReceiptImageStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReceiptImageStore receiptImageStore;

    // Use absolute path with user.dir (application root directory)
    private static final String RECEIPT_UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/receipts/";

    // Save the receipt image and return the filename or null if the image is empty
    public String saveReceiptImage(MultipartFile receiptImage) throws IOException {
        if (receiptImage != null && !receiptImage.isEmpty()) {
            String filename = System.currentTimeMillis() + "_" + receiptImage.getOriginalFilename();
            receiptImageStore.setContent(filename, receiptImage.getInputStream());
            return filename;  // Return filename to be stored in the order entity
        }
        return null;  // Return null if no image was provided
    }

    @Transactional
    public Order saveOrder(Order order, String email) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found for email: " + email);
        }

        order.setUser(user); // Associate order with the user
        order.setOrderDate(LocalDateTime.now());

        // Save the order
        Order savedOrder = orderRepository.save(order);

        try {
            // Deduct stock for each ordered item
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                int orderedQuantity = orderItem.getQuantity();

                // Deduct stock
                if (product.getStock() < orderedQuantity) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }

                product.setStock(product.getStock() - orderedQuantity);
                productRepository.save(product); // Save updated stock
            }
        } catch (Exception e) {
            // Log error and rethrow for transaction rollback
            System.err.println("Error processing order items: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return savedOrder;
    }

    // Find an order by its ID
    public Optional<Order> findById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    // Retrieve all orders
    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    // Retrieve all orders by user's email
    public List<Order> findAllByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return new ArrayList<>(); // Return empty list if user not found
        }
        return orderRepository.findByUser(user); // Fetch orders by user entity
    }
}