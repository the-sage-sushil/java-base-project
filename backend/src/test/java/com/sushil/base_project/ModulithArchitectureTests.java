package com.sushil.base_project;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTests {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(BaseProjectApplication.class).verify();
    }
}
