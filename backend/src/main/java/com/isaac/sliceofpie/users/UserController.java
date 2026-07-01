package com.isaac.sliceofpie.users;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isaac.sliceofpie.users.UserDtos.MeResponse;

@RestController
@RequestMapping("/me")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public MeResponse me(Authentication authentication) {
        String username = authentication.getName();
        return new MeResponse(username);
    } 
}
