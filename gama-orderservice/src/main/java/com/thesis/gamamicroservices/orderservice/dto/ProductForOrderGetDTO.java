package com.thesis.gamamicroservices.orderservice.dto;

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

}
