package com.catchthethief.authentication.controller;
/**
 * @author Mohit Kumar
 */

import com.catchthethief.authentication.exception.BadRequestException;
import com.catchthethief.authentication.model.AuthProvider;
import com.catchthethief.authentication.model.User;
import com.catchthethief.authentication.payload.ApiResponse;
import com.catchthethief.authentication.payload.AuthResponse;
import com.catchthethief.authentication.payload.LoginRequest;
import com.catchthethief.authentication.payload.SignUpRequest;
import com.catchthethief.authentication.repository.UserRepository;
import com.catchthethief.authentication.security.TokenAuthenticationFilter;
import com.catchthethief.authentication.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.createToken(authentication);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        if(userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email address already in use.");
        }

        // Creating user's account
        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword());
        user.setProvider(AuthProvider.local);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User result = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/user/me")
                .buildAndExpand(result.getId()).toUri();

        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "User registered successfully"));
    }

    @GetMapping("/authenticateRequest")
    public ResponseEntity<?> authenticateRequest(HttpServletRequest request) {

        if(null != request && TokenAuthenticationFilter.VALID_REQUEST.equals(request.getAttribute(TokenAuthenticationFilter.AUTHENTICATED_REQUEST))){
            return ResponseEntity.ok("success");
        }
        return new ResponseEntity<>(
                "failed", HttpStatus.UNAUTHORIZED);
    }

}
