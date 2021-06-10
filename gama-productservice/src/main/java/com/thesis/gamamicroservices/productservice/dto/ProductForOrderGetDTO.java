package com.thesis.gamamicroservices.productservice.dto;

import com.thesis.gamamicroservices.productservice.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductForOrderGetDTO {
    private int productId;
    private String name;
    private Double price;
    private Double promotionPrice;
    private float weight;

    public ProductForOrderGetDTO(Product product) {
        this.productId = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.promotionPrice = product.getPromotionPrice();
        this.weight = product.getWeight();
    }
}
