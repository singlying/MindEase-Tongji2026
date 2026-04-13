package com.mindease.controller;

import com.mindease.common.Result;
import com.mindease.model.entity.User;
import com.mindease.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public Result<List<User>> getAllUsers() {
        return Result.success(userService.findAll());
    }
    
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(Result::success)
                .orElse(Result.error("User not found"));
    }
    
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        if (userService.existsByUsername(user.getUsername())) {
            return Result.error("Username already exists");
        }
        return Result.success("User created", userService.save(user));
    }
    
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.findById(id)
                .map(existingUser -> {
                    existingUser.setEmail(user.getEmail());
                    existingUser.setPhone(user.getPhone());
                    existingUser.setAvatar(user.getAvatar());
                    return Result.success("User updated", userService.save(existingUser));
                })
                .orElse(Result.error("User not found"));
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return Result.success("User deleted", null);
    }
}
