package info.danilocangucu.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import info.danilocangucu.configs.JwtService;
import info.danilocangucu.models.User;
import info.danilocangucu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Validator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    @Autowired
    private Validator validator;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(User request) {
        Set<ConstraintViolation<User>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            buildStringAndThrowException(violations);
        }

        var user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role("ROLE_USER")
            .build();
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
            .token(jwtToken)
            .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            buildStringAndThrowException(violations);
        }

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );
        var user = userRepository.findByEmail(request.getEmail())
            .orElseThrow();

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
            .token(jwtToken)
            .build();
    }

    private static <T> void buildStringAndThrowException(Set<ConstraintViolation<T>> violations) {
        System.out.println("buildString");
        StringBuilder sb = new StringBuilder();

        for (ConstraintViolation<T> violation : violations) {
            sb.append(violation.getMessage()).append(". ");
        }
        throw new ConstraintViolationException("Error occurred: " + sb, violations);
    }

    public static ResponseEntity<?> responseWithError(String message) {
        Map<String, String> errorResponse = Collections.singletonMap("error", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

}
