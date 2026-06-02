package com.jadakeel.common;

import org.junit.jupiter.api.Test;

class CommonLibraryApplicationTests {

	@Test
	void applicationClassIsPresent() {
		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Class.forName("com.jadakeel.common.CommonApplication"));
	}

}
