package com.isaac.sliceofpie.auth;

import com.isaac.sliceofpie.users.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
}