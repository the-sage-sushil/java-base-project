package com.sushil.base_project.auth.models;

/**
 * LoginResponseDTO
 */
public record LoginResponseDTO(

        String username,
        String jwt,
        long expiresIn

) {

}
