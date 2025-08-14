package com.server.game.service.user;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

// import org.springframework.security.access.prepost.PostAuthorize;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.core.context.SecurityContextHolder;


import org.springframework.stereotype.Service;

import com.server.game.dto.request.RegisterRequest;
import com.server.game.exception.http.DataNotFoundException;
import com.server.game.exception.http.FieldExistedExeption;
import com.server.game.exception.http.UnauthorizedException;
import com.server.game.mapper.UserMapper;
import com.server.game.model.user.User;
import com.server.game.repository.mongo.UserRepository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @NonFinal
    private Map<String, String> activeSessions = new ConcurrentHashMap<>();

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
    // @PostAuthorize("hasRole('ADMIN') or returnObject.id == authentication.name") // authentication.name is the ID of the authenticated user
    public User getUserById(String id) {
        return getUserByIdInternal(id);
    }



    public User register(RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw new FieldExistedExeption("User with username " + username + " already exists");
        }
        String password = registerRequest.getPassword();
        String encodedPassword = passwordEncoder.encode(password);
        User user = userMapper.toUser(registerRequest);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        return user;
    }

    public User validateCredentials(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Username does not exist"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Incorrect password");
        }
    
        // Check if user is already logged in
        if (activeSessions.containsKey(user.getId())) {
            // Option 1: Prevent login
            throw new UnauthorizedException("Account is already logged in from another device");
            
            // Option 2: Force logout previous session (commented out)
            // String previousSessionId = activeSessions.get(user.getId());
            // invalidatePreviousSession(previousSessionId);
        }
        return user;
    }

    /**
     * Register an active session for a user
     */
    public void registerUserSession(String userId, String sessionId) {
        activeSessions.put(userId, sessionId);
    }

    /**
     * Remove a user's active session
     */
    public void removeUserSession(String userId) {
        activeSessions.remove(userId);
    }

    /**
     * Check if a user has an active session
     */
    public boolean hasActiveSession(String userId) {
        return activeSessions.containsKey(userId);
    }

    public boolean isUserExist(String id) {
        return userRepository.existsById(id);
    }

    public String getUsernameById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("User with ID " + id + " not found"))
                .getUsername();
    }
}

