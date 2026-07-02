package com.isaac.sliceofpie.users;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
import com.isaac.sliceofpie.users.UserDtos.MeResponse;

@RestController
@RequestMapping("/me")
public class UserController {

    public UserController() {}

    @GetMapping
    public MeResponse me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new MeResponse(principal.username());
    } 
}
