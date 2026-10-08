package com.sushil.base_project.auth.models;

import java.util.Set;

/**
 * LoginResponseDTO
 */
public record LoginResponseDTO(


    String username,
    String email,
    Set<String> roles

) {

}
