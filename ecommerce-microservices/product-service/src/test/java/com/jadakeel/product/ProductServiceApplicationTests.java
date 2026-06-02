package com.jadakeel.product;

import org.junit.jupiter.api.Test;

class ProductServiceApplicationTests {
    @Test
    void applicationClassIsPresent() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Class.forName("com.jadakeel.product.ProductServiceApplication"));
    }
}
