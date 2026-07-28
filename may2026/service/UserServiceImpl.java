package com.example.scaler.bms.may2026.service;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.scaler.bms.may2026.exception.InvalidRequestException;
import com.example.scaler.bms.may2026.model.User;
import com.example.scaler.bms.may2026.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new user.
     */
    public User registerUser(String userEmail, String password)
            throws InvalidRequestException {

        Optional<User> existingUser = userRepository.findByEmail(userEmail);

        if (existingUser.isPresent()) {
            throw new InvalidRequestException("User is already registered.");
        }

        User user = new User();

        user.setEmail(userEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedAt(new Date());
        user.setDeleted(false);

        User savedUser = userRepository.save(user);

        log.info("User registered successfully with id : {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Login existing user.
     */
    public boolean login(String userEmail, String password)
            throws InvalidRequestException {

        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isEmpty()) {
            throw new InvalidRequestException("User not found.");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidRequestException("Invalid password.");
        }

        log.info("User logged in successfully : {}", user.getEmail());

        return true;
    }

}