@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "user", "core::security", "core::audit"
})
@org.springframework.modulith.NamedInterface("auth")
package com.sushil.base_project.auth;
