package com.sushil.base_project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=test-only-secret-key-with-at-least-32-bytes")
class BaseProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
