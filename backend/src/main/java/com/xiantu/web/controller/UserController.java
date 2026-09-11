package com.xiantu.web.controller;

import com.xiantu.common.Result;
import com.xiantu.service.UserService;
import com.xiantu.web.dto.UserProfile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserProfile> me(@AuthenticationPrincipal(expression = "username") String username) {
        return Result.ok(userService.me(username));
    }
}
