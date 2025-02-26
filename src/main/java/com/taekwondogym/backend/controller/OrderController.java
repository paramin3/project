package com.taekwondogym.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taekwondogym.backend.model.Address;
import com.taekwondogym.backend.model.CartItem;
import com.taekwondogym.backend.model.Order;
import com.taekwondogym.backend.model.OrderItem;
import com.taekwondogym.backend.service.AddressService;
import com.taekwondogym.backend.service.CartService;
import com.taekwondogym.backend.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final AddressService addressService;
    private final HttpSession session;

    @Autowired
    public OrderController(OrderService orderService, CartService cartService, AddressService addressService, HttpSession session) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.addressService = addressService;
        this.session = session;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String surname,
            @RequestParam String telephone,
            @RequestParam String deliveryType,
            @RequestParam(required = false) Long addressId,
            @RequestParam(required = false) String shippingAddress,
            @RequestParam("receiptImage") MultipartFile receiptImage) {
        try {
            System.out.println("Creating order for email: " + email);
            
            // Validate receipt image
            if (receiptImage == null || receiptImage.isEmpty()) {
                return ResponseEntity.badRequest().body("Receipt image is required");
            }
            
            // Check if the cart is empty
            List<CartItem> cartItems = cartService.getCartItems(email, null);
            if (cartItems.isEmpty()) {
                String sessionId = (String) session.getAttribute("sessionId");
                if (sessionId != null) {
                    cartItems = cartService.getCartItems(null, sessionId);
                    System.out.println("Using sessionId: " + sessionId + ", Cart items count: " + cartItems.size());
                }
                if (cartItems.isEmpty()) {
                    System.out.println("Cart is empty for email: " + email + " and sessionId: " + sessionId);
                    return ResponseEntity.badRequest().body("Cart is empty");
                }
            }

            // Calculate total amount
            double totalAmount = cartItems.stream()
                    .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                    .sum();

            // Create order object
            Order order = new Order();
            order.setName(name);
            order.setSurname(surname);
            order.setTelephone(telephone);
            order.setDeliveryType(deliveryType);
            order.setTotalAmount(totalAmount);
            order.setOrderDate(LocalDateTime.now());

            // Handle shipping address
            if ("shipping".equalsIgnoreCase(deliveryType)) {
                try {
                    if (addressId != null) {
                        Address existingAddress = addressService.findById(addressId)
                                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));
                        order.setHomeAddress(existingAddress.getHomeAddress());
                        order.setPostcode(existingAddress.getPostcode());
                        order.setCity(existingAddress.getCity());
                        order.setDistrict(existingAddress.getDistrict());
                        order.setSubDistrict(existingAddress.getSubDistrict());
                        order.setRoad(existingAddress.getRoad());
                        order.setSoi(existingAddress.getSoi());
                        order.setMoo(existingAddress.getMoo());
                    } else if (shippingAddress != null && !shippingAddress.isEmpty()) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Address newAddress = objectMapper.readValue(shippingAddress, Address.class);
                        
                        // Validate address fields
                        if (newAddress.getHomeAddress() == null || newAddress.getPostcode() == null) {
                            return ResponseEntity.badRequest().body("Invalid shipping address details");
                        }
                        
                        Address savedAddress = addressService.saveAddress(newAddress, email);
                        order.setHomeAddress(savedAddress.getHomeAddress());
                        order.setPostcode(savedAddress.getPostcode());
                        order.setCity(savedAddress.getCity());
                        order.setDistrict(savedAddress.getDistrict());
                        order.setSubDistrict(savedAddress.getSubDistrict());
                        order.setRoad(savedAddress.getRoad());
                        order.setSoi(savedAddress.getSoi());
                        order.setMoo(savedAddress.getMoo());
                    } else {
                        return ResponseEntity.badRequest().body("Shipping address is required for delivery type 'shipping'");
                    }
                } catch (Exception e) {
                    System.err.println("Error processing shipping address: " + e.getMessage());
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing shipping address: " + e.getMessage());
                }
            }

            // Save receipt image
            try {
                String receiptFilePath = orderService.saveReceiptImage(receiptImage);
                order.setReceiptFilePath(receiptFilePath);
            } catch (IOException e) {
                System.err.println("Error saving receipt image: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving receipt image: " + e.getMessage());
            }

            // Add order items
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setPrice(cartItem.getProduct().getPrice());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setOrder(order);
                order.getOrderItems().add(orderItem);
            }

            // Save order
            try {
                Order savedOrder = orderService.saveOrder(order, email);
                cartService.clearCart(email, null);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
            } catch (RuntimeException e) {
                System.err.println("Error saving order: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error saving order: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Unexpected error creating order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long orderId) {
        Optional<Order> order = orderService.findById(orderId);
        return order.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.findAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable String email) {
        List<Order> orders = orderService.findAllByEmail(email);
        return ResponseEntity.ok(orders);
    }
}