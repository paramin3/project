package com.taekwondogym.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDTO {
    private String name;
    private String surname;
    private String telephone;
    private String deliveryType;
    private double totalAmount;
    private LocalDateTime orderDate;
    private List<OrderItemDTO> items;

    public OrderResponseDTO(String name, String surname, String telephone, String deliveryType,
                            double totalAmount, LocalDateTime orderDate, List<OrderItemDTO> items) {
        this.name = name;
        this.surname = surname;
        this.telephone = telephone;
        this.deliveryType = deliveryType;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.items = items;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }
}
