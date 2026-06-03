package com.jadakeel.common;

import com.jadakeel.common.event.order.OrderCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommonLibraryApplicationTests {

	@Test
	void eventClassIsPresent() {
		assertNotNull(OrderCreatedEvent.class);
	}

}
