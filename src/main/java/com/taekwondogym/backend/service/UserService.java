package com.taekwondogym.backend.service;

import com.taekwondogym.backend.model.Role;
import com.taekwondogym.backend.model.User;
import com.taekwondogym.backend.repository.RoleRepository;
import com.taekwondogym.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    public User registerUser(User user) throws Exception {
        if (userRepository.existsByEmail(user.getEmail())) { // ตรวจสอบ email ซ้ำ
            throw new Exception("Email is already registered!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword())); // เข้ารหัสรหัสผ่าน
        Role userRole = roleRepository.findByName("ROLE_USER");
        user.setRole(userRole);
        return userRepository.save(user);
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email); 
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException { // Use email instead of username
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }

        // Since we have only one role per user, directly set the authority based on the single role
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().getName());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Updated to use email instead of username
                user.getPassword(),
                Collections.singleton(authority) // Single authority instead of a collection
        );
    }
}
