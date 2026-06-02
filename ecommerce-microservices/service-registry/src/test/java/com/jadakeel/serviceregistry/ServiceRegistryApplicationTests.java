package com.jadakeel.serviceregistry;

import org.junit.jupiter.api.Test;

class ServiceRegistryApplicationTests {

	@Test
	void applicationClassIsPresent() {
		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Class.forName("com.jadakeel.serviceregistry.ServiceRegistryApplication"));
	}

}
