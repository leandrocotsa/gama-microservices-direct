package com.thesis.gamamicroservices.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class InventoryGetDTO {
    private String warehouseName;
    private int stockAmount;

}