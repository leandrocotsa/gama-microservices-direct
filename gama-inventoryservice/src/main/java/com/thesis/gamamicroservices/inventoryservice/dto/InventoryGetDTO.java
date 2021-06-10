package com.thesis.gamamicroservices.inventoryservice.dto;

import com.thesis.gamamicroservices.inventoryservice.model.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InventoryGetDTO {
    private String warehouseName;
    private int stockAmount;

    public InventoryGetDTO(Inventory inventory) {
        this.warehouseName = inventory.getWarehouse().getName();
        this.stockAmount = inventory.getStockAmount();
    }
}