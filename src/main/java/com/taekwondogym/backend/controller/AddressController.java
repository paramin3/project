package com.taekwondogym.backend.controller;

import com.taekwondogym.backend.model.Address;
import com.taekwondogym.backend.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    // ดึง email จากผู้ใช้ที่ล็อกอินอยู่
    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername(); // สมมติว่า username คือ email
        }
        return null;
    }

    // Get all addresses for the current user
    @GetMapping
    public ResponseEntity<List<Address>> getUserAddresses() {
        String email = getCurrentUserEmail();
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Address> addresses = addressService.getUserAddresses(email);
        return ResponseEntity.ok(addresses);
    }

    // Get a specific address by ID
    @GetMapping("/{id}")
    public ResponseEntity<Address> getAddressById(@PathVariable Long id) {
        Optional<Address> address = addressService.findById(id);
        return address.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Create a new address
    @PostMapping
    public ResponseEntity<Address> addAddress(@RequestBody Address address) {
        String email = getCurrentUserEmail();
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Address savedAddress = addressService.saveAddress(address, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAddress);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // User not found
        }
    }

    // Update an existing address
    @PutMapping("/{id}")
    public ResponseEntity<Address> updateAddress(
            @PathVariable Long id,
            @RequestBody Address addressDetails) {
        String email = getCurrentUserEmail();
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Address> existingAddressOpt = addressService.findById(id);
        if (existingAddressOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Address existingAddress = existingAddressOpt.get();
        existingAddress.setName(addressDetails.getName());
        existingAddress.setHomeAddress(addressDetails.getHomeAddress());
        existingAddress.setRoad(addressDetails.getRoad());
        existingAddress.setSoi(addressDetails.getSoi());
        existingAddress.setMoo(addressDetails.getMoo());
        existingAddress.setSubDistrict(addressDetails.getSubDistrict());
        existingAddress.setDistrict(addressDetails.getDistrict());
        existingAddress.setCity(addressDetails.getCity());
        existingAddress.setPostcode(addressDetails.getPostcode());
        existingAddress.setDefault(addressDetails.isDefault());

        try {
            Address updatedAddress = addressService.saveAddress(existingAddress, email);
            return ResponseEntity.ok(updatedAddress);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // User not found
        }
    }

    // Delete an address
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        Optional<Address> address = addressService.findById(id);
        if (address.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}