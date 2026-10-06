package com.sushil.base_project.user.Models;

import java.util.Set;

import com.sushil.base_project.user.Entities.Role;

/**
 * LoginResponseDTO
 */
public record LoginResponseDTO(


    String username,
    String email,
    Set<Role> roles

) {

}
