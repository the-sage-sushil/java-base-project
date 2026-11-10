package com.sushil.base_project.user.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sushil.base_project.user.Entities.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
