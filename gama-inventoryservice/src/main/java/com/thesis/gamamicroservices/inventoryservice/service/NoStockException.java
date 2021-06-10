package com.thesis.gamamicroservices.inventoryservice.service;

public class NoStockException extends Exception {

    NoStockException(String message) {
        super(message);
    }
}