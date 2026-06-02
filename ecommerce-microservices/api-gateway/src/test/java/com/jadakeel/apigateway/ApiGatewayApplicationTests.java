package com.jadakeel.apigateway;

import org.junit.jupiter.api.Test;

class ApiGatewayApplicationTests {

	@Test
	void applicationClassIsPresent() {
		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Class.forName("com.jadakeel.apigateway.ApiGatewayApplication"));
	}

}
