package dev.enrique.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dao.UserRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    @GetMapping("/{id}/password")
    public ResponseEntity<String> getUserPassword(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(user -> ResponseEntity.ok(user.getPassword()))
            .orElseGet(() -> ResponseEntity.notFound().build());
        
    }
}
