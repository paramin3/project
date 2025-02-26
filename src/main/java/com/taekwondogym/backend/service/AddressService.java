package com.taekwondogym.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.taekwondogym.backend.model.Address;
import com.taekwondogym.backend.model.User;
import com.taekwondogym.backend.repository.AddressRepository;
import com.taekwondogym.backend.repository.UserRepository;

@Service
public class AddressService {
    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Address saveAddress(Address address, String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        address.setUser(user);
        if (address.isDefault()) {
            // Remove default flag from other addresses
            addressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(defaultAddress -> {
                    defaultAddress.setDefault(false);
                    addressRepository.save(defaultAddress);
                });
        }
        return addressRepository.save(address);
    }
    
    public List<Address> getUserAddresses(String email) {
        User user = userRepository.findByEmail(email);
        return user != null ? addressRepository.findByUser(user) : new ArrayList<>();
    }

    public Optional<Address> findById(Long id) {
        return addressRepository.findById(id);
    }

    public void deleteAddress(Long id) {
        addressRepository.deleteById(id);
    }
}