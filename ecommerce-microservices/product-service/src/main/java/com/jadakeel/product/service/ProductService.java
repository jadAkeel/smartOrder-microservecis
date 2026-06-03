package com.jadakeel.product.service;

import com.jadakeel.product.dto.ProductRequest;
import com.jadakeel.product.dto.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductResponse> getProducts(String category, String search);
    ProductResponse getProduct(UUID id);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(UUID id, ProductRequest request);
    void deleteProduct(UUID id);
}
