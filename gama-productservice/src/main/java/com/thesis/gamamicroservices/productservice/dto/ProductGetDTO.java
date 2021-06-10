package com.thesis.gamamicroservices.productservice.dto;

import com.thesis.gamamicroservices.productservice.model.Product;
import com.thesis.gamamicroservices.productservice.model.SpecificationValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductGetDTO {
    private int productId;
    private String name;
    private String description;
    private Double price;
    private Double promotionPrice;
    private String brandName;
    private String categoryName;
    private List<SpecificationValueSetDTO> specificationValues;
    private List<ReviewGetDTO> reviews;
    private List<InventoryGetDTO> inventories;

    public ProductGetDTO(Product product, List<ReviewGetDTO> reviews, List<InventoryGetDTO> inventories) {
        this.productId = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.promotionPrice = product.getPromotionPrice();
        this.brandName = product.getBrand().getName();
        this.categoryName = product.getCategory().getName();
        this.specificationValues = product.getSpecificationValues().stream().map(SpecificationValueSetDTO::new).collect(Collectors.toList());
        this.reviews = reviews;
        this.inventories = inventories;
    }


    public ProductGetDTO(Product product) {
        this.productId = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.promotionPrice = product.getPromotionPrice();
        this.brandName = product.getBrand().getName();
        this.categoryName = product.getCategory().getName();
        this.specificationValues = product.getSpecificationValues().stream().map(SpecificationValueSetDTO::new).collect(Collectors.toList());
    }

}
