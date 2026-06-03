package com.jadakeel.product.service;

import com.jadakeel.product.dto.ProductRequest;
import com.jadakeel.product.dto.ProductResponse;
import com.jadakeel.product.model.Product;
import com.jadakeel.product.repository.ProductRepository;
import com.jadakeel.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository);
    }

    @Test
    void createProduct_shouldSaveAndReturnProduct() {
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setPrice(BigDecimal.valueOf(10.99));
        request.setCategory("Food");
        request.setAvailable(true);

        Product savedProduct = new Product();
        savedProduct.setId(UUID.randomUUID());
        savedProduct.setName("Test Product");
        savedProduct.setDescription("Test Description");
        savedProduct.setPrice(BigDecimal.valueOf(10.99));
        savedProduct.setCategory("Food");
        savedProduct.setAvailable(true);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("Test Product", response.getName());
        assertEquals(BigDecimal.valueOf(10.99), response.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getProduct_shouldReturnProductWhenFound() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setName("Existing Product");
        product.setPrice(BigDecimal.valueOf(5.99));

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProduct(id);

        assertNotNull(response);
        assertEquals("Existing Product", response.getName());
    }

    @Test
    void getProduct_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> productService.getProduct(id));
    }

    @Test
    void getProducts_shouldFilterByCategory() {
        String category = "Food";
        when(productRepository.findByCategoryIgnoreCase(category)).thenReturn(List.of(new Product()));

        List<ProductResponse> results = productService.getProducts(category, null);

        assertEquals(1, results.size());
        verify(productRepository, times(1)).findByCategoryIgnoreCase(category);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getProducts_shouldSearchByName() {
        String search = "Burger";
        when(productRepository.findByNameContainingIgnoreCase(search)).thenReturn(List.of(new Product()));

        List<ProductResponse> results = productService.getProducts(null, search);

        assertEquals(1, results.size());
        verify(productRepository, times(1)).findByNameContainingIgnoreCase(search);
    }

    @Test
    void getProducts_shouldReturnAllWhenNoFilters() {
        when(productRepository.findAll()).thenReturn(List.of(new Product(), new Product()));

        List<ProductResponse> results = productService.getProducts(null, null);

        assertEquals(2, results.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void deleteProduct_shouldDeleteExistingProduct() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.deleteProduct(id);

        verify(productRepository, times(1)).delete(product);
    }

    @Test
    void updateProduct_shouldUpdateAndReturnProduct() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setName("Old Name");
        existing.setPrice(BigDecimal.valueOf(1.00));

        ProductRequest request = new ProductRequest();
        request.setName("New Name");
        request.setDescription("New Desc");
        request.setPrice(BigDecimal.valueOf(2.00));
        request.setCategory("Drinks");
        request.setAvailable(true);

        Product updated = new Product();
        updated.setId(id);
        updated.setName("New Name");
        updated.setDescription("New Desc");
        updated.setPrice(BigDecimal.valueOf(2.00));
        updated.setCategory("Drinks");
        updated.setAvailable(true);

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        ProductResponse response = productService.updateProduct(id, request);

        assertEquals("New Name", response.getName());
        assertEquals("Drinks", response.getCategory());
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
