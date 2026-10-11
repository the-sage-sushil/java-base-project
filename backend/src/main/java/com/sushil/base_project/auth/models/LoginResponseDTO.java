package com.sushil.base_project.auth.models;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

/**
 * LoginResponseDTO
 */
public record LoginResponseDTO(

        String username,
        String jwt,
        long expiresIn

) {

}
