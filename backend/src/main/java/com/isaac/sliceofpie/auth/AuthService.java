package com.isaac.sliceofpie.auth;

import com.isaac.sliceofpie.users.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public void register(String username, String password) {
        String hashed = encoder.encode(password);
        User user = new User(username, hashed);
        userRepository.save(user);
    }

    public void login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Password");
        }
    }
}