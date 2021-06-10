package com.thesis.gamamicroservices.productservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thesis.gamamicroservices.productservice.dto.ProductForOrderGetDTO;
import com.thesis.gamamicroservices.productservice.dto.ProductGetDTO;
import com.thesis.gamamicroservices.productservice.dto.ProductSetDTO;
import com.thesis.gamamicroservices.productservice.service.NoDataFoundException;
import com.thesis.gamamicroservices.productservice.service.ProductService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private static final String PRODUCT_CREATED_LOG = "A Product was created";
    private static final String PRODUCT_DELETED_LOG = "Product: {} was deleted";
    private static final String PRODUCT_UPDATED_LOG = "Product: {} was updated";

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public void createProduct(@Valid @RequestBody ProductSetDTO productSetDTO) throws NoDataFoundException, JsonProcessingException {
        this.productService.createProduct(productSetDTO);
        logger.info(PRODUCT_CREATED_LOG);
    }

    @DeleteMapping(path="/{productID}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable int productID) throws NoDataFoundException {
        this.productService.deleteProduct(productID);
        logger.info(PRODUCT_DELETED_LOG, productID);
    }

    @PatchMapping(path="/{productID}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public void editProduct(@RequestBody Map<String, Object> updates, @PathVariable("productID") int productID) throws NoDataFoundException {
        this.productService.editProduct(updates, productID);
        logger.info(PRODUCT_UPDATED_LOG, productID);
    }



    @GetMapping(path="/list/{productIds}")
    @ResponseStatus(HttpStatus.OK)
    public List<ProductForOrderGetDTO> getListProducts(@PathVariable List<Integer> productIds) throws NoDataFoundException {
        return this.productService.getListOfProducts(productIds);
    }

    @GetMapping(path="/exists/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public Boolean productExists(@PathVariable Integer productId) throws NoDataFoundException {
        return this.productService.productExists(productId);
    }





    @GetMapping(path="/{id}")
    public ResponseEntity<ProductGetDTO> getProductById(@PathVariable int id) throws NoDataFoundException {
        return ResponseEntity.ok(this.productService.getProductDetailsById(id));
    }




}
