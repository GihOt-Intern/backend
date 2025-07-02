package com.server.game.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.security.access.prepost.PostAuthorize;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.core.context.SecurityContextHolder;


import org.springframework.stereotype.Service;

import com.server.game._dto.request.CreateUserRequest;
import com.server.game.exception.*;
import com.server.game.mapper.UserMapper;
import com.server.game.model.User;
import com.server.game.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    // @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserInfo() {
        String idFromAuth = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(idFromAuth)
                .orElseThrow(() -> new DataNotFoundException("User with ID " + idFromAuth + " not found"));
    }

    // Used internally, no security check
    public User getUserByIdInternal(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new DataNotFoundException("User with ID " + id + " not found"));
    }

    // Expose this method with security check
    @PostAuthorize("hasRole('ADMIN') or returnObject.id == authentication.name") // authentication.name is the ID of the authenticated user
    public User getUserById(String id) {
        return getUserByIdInternal(id);
    }


    @PostAuthorize("hasRole('ADMIN') or returnObject.id == authentication.name") // authentication.name is the ID of the authenticated user
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("User with email " + email + " not found"));
    }

    public User createUser(CreateUserRequest createUserRequest) {
        String email = createUserRequest.getEmail();

        if (userRepository.existsByEmail(email)) {
            throw new FieldExistedExeption("User with email " + email + " already exists");
        }

        String password = createUserRequest.getPassword();
        String encodePassword =  passwordEncoder.encode(password);
        // System.out.println(">>>>>>" + encodePassword);


        User user = userMapper.toUser(createUserRequest);
        user.setPassword(encodePassword);

        userRepository.save(user);
        return user;
    }

    public User validateCredentials(String email, String password) {
        User user =  userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Incorrect email"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Incorrect password");
        }
        return user;
    }

    public boolean isUserExist(String id) {
        return userRepository.existsById(id);
    }
}

