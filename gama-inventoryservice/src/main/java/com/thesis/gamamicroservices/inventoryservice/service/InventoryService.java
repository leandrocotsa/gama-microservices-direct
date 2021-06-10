package com.thesis.gamamicroservices.inventoryservice.service;

import com.thesis.gamamicroservices.inventoryservice.dto.InventoryGetDTO;
import com.thesis.gamamicroservices.inventoryservice.dto.WarehouseSetDTO;
import com.thesis.gamamicroservices.inventoryservice.model.Inventory;
import com.thesis.gamamicroservices.inventoryservice.model.Warehouse;
import com.thesis.gamamicroservices.inventoryservice.repository.InventoryRepository;
import com.thesis.gamamicroservices.inventoryservice.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.*;

@Transactional
@Service
public class InventoryService {

    private static final String PRODUCT_CHECK_URL = "http://localhost:8080/products/exists/";

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository, WarehouseRepository warehouseRepository, RestTemplate restTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.restTemplate = restTemplate;
    }


    public List<Inventory> findInventoriesByWarehouse(int warehouseID) {
        return inventoryRepository.findAllByWarehouseId(warehouseID);
    }

    public int getStockAmountOfProduct(int productId) {
        List<Inventory> inventories = inventoryRepository.findAllByProductId(productId);
        int totalStock = 0;
        for(Inventory i : inventories) {
            totalStock += i.getStockAmount();
        }
        return totalStock;
    }

    public List<Inventory> getInventoriesOfProduct(int productId) {
        return inventoryRepository.findAllByProductId(productId);
    }

    public List<InventoryGetDTO> getInventoriesDTOOfProduct(int productId) {

        List<InventoryGetDTO> inventories = new ArrayList<>();
        inventoryRepository.findAllByProductId(productId).forEach(i -> inventories.add(new InventoryGetDTO(i)));

        return inventories;
    }

    public void addStock(int productID, int warehouseID, int qty) throws NoDataFoundException {

        Boolean product_exists = false;

        try {
            product_exists = restTemplate.getForEntity(PRODUCT_CHECK_URL + productID, Boolean.class).getBody();

        } catch (HttpStatusCodeException ex) {
            System.out.println(ex.getStatusCode().toString());
            throw new ServiceDownException("Product Service is currently down. Try again later");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceDownException("Product Service is currently down. Try again later");
        }

        if(product_exists){
            Optional<Inventory> inventory = inventoryRepository.findByProductIdAndWarehouseId(productID, warehouseID);
            if (inventory.isPresent()) {
                inventory.get().setStockAmount(inventory.get().getStockAmount() + qty);
                inventoryRepository.save(inventory.get());
                //rabbitTemplate.convertAndSend(exchange.getName(), "inventory", new InventoryUpdatedMessage(inventory.get()));
            } else {
                try {
                    Optional<Warehouse> w = warehouseRepository.findById(warehouseID);
                    if (w.isPresent()) {
                        Inventory newInv = new Inventory(w.get(), productID, qty);
                        inventoryRepository.save(newInv);
                    } else {
                        throw new NoDataFoundException("There is no warehouse with id" + warehouseID);
                    }
                } catch (NoDataFoundException e) {
                    throw e;
                }
            }

        } else {
            throw new NoDataFoundException("There is no product with that id");
        }


    }

    public void editStock(int productID, int warehouseID, int qty) throws NoDataFoundException {
        Optional<Inventory> inventory = inventoryRepository.findByProductIdAndWarehouseId(productID, warehouseID);
        if (inventory.isPresent()) {
            inventory.get().setStockAmount(qty);
            inventoryRepository.save(inventory.get());
            //rabbitTemplate.convertAndSend(exchange.getName(), "inventory", new InventoryUpdatedMessage(inventory.get()));
        } else {
            throw new NoDataFoundException("There's no inventory for Product with id " + productID + " and wahrehouse with id" + warehouseID);
        }
    }


    public void reserveStock(Map<Integer,Integer> products_qty) throws NoStockException {
        //loop a ver se todos têm stock e outro loop a retirar um e guardar

        for (Map.Entry<Integer, Integer> entry : products_qty.entrySet()) {

            if (this.getStockAmountOfProduct(entry.getKey()) < entry.getValue()) {
                throw new NoStockException("Not that much stock available for Product: " + entry.getKey());
            }
        }
        for (Map.Entry<Integer, Integer> entry : products_qty.entrySet()) {
            int qtyLeft = entry.getValue();
            for (int y = 0; y < getInventoriesOfProduct(entry.getKey()).size(); ++y) { //teve que ser loop assim pq com iterator na podia modificar elemento enquanto iterava
                if (qtyLeft == 0) {
                    return;
                }
                Inventory i = getInventoriesOfProduct(entry.getKey()).get(y);
                if (qtyLeft <= i.getStockAmount()) {
                    i.setStockAmount(i.getStockAmount() - qtyLeft);
                    qtyLeft = 0;
                } else {
                    qtyLeft = qtyLeft - i.getStockAmount();
                    i.setStockAmount(0);
                    //devia poder criar notificação para os admins, com scheduled jobs que verifica stocks de x em x tempo?
                    //com um thread constantemente a verificar se stock=0?
                }
                this.inventoryRepository.save(i);
                //rabbitTemplate.convertAndSend(exchange.getName(), "inventory", new InventoryUpdatedMessage(i));
                //tenho que por numa string na order de quais armazens stock foi retirado
            }

        }

    }


    public void createWarehouse(WarehouseSetDTO warehouseSetDTO) throws AlreadyExistsException {

        Optional<Warehouse> existingWarehouse = this.warehouseRepository.findByName(warehouseSetDTO.getName());
        if (existingWarehouse.isEmpty()) {
            Warehouse warehouse = new Warehouse((warehouseSetDTO));
            warehouseRepository.save(warehouse);
            //rabbitTemplate.convertAndSend(exchange.getName(), "inventory", new WarehouseCreatedMessage(warehouse));
        } else {
            throw new AlreadyExistsException("There's a Warehouse with that name");
        }
    }


    public void deleteWarehouse(int id) throws NoDataFoundException {
        if (warehouseRepository.existsById(id)) { //evita que tenha de fazer um fetch extra
            warehouseRepository.deleteById(id);
            //rabbitTemplate.convertAndSend(exchange.getName(), "inventory", new WarehouseDeletedMessage(id));
        } else {
            throw new NoDataFoundException("There's no Warehouse with that id");
        }
    }

    public void editWarehouse(Map<String, Object> updates, int id) throws NoDataFoundException {
        Optional<Warehouse> warehouse = warehouseRepository.findById(id);
        if (warehouse.isPresent()) {
            try {
                // Map key is field name, v is value
                updates.forEach((k, v) -> {
                    // use reflection to get field k on manager and set it to value v
                    Field field = ReflectionUtils.findField(Warehouse.class, k);
                    field.setAccessible(true);
                    ReflectionUtils.setField(field, warehouse.get(), v);
                });
                warehouseRepository.save(warehouse.get());
            } catch (Exception e) {
                throw new NoDataFoundException("Invalid arguments");
            }
        } else {
            throw new NoDataFoundException("There's no Warehouse with that id");
        }

    }

    public void deleteInventoriesByProduct(int productId) {
        inventoryRepository.deleteByProductId(productId);
    }
}
