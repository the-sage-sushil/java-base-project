package com.sushil.base_project.user.Controller;

import org.springframework.web.bind.annotation.RestController;

import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Models.LoginRequestDTO;
import com.sushil.base_project.user.Models.LoginResponseDTO;
import com.sushil.base_project.user.Models.RegisterRequestDTO;
import com.sushil.base_project.user.Services.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.net.http.HttpClient;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController()
@RequestMapping("/api/auth")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @PostMapping("register")
    public ResponseEntity<Void> createUser(@RequestBody @Valid RegisterRequestDTO request) {

        userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    } 


    @PostMapping("login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {

        User user = userService.login(request);
        LoginResponseDTO response = new LoginResponseDTO(user.getUsername(), user.getEmail(), user.getRoles());
        
        return ResponseEntity.ok(response);
        
    }
    

}
