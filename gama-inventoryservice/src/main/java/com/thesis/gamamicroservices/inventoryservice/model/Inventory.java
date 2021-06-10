package com.thesis.gamamicroservices.inventoryservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name="inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    Warehouse warehouse;
    int productId;

    int stockAmount;

    public Inventory(Warehouse warehouse, int productId, int stockAmount) {
        this.warehouse = warehouse;
        this.productId = productId;
        this.stockAmount = stockAmount;
    }
}
