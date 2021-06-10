package com.thesis.gamamicroservices.inventoryservice.service;

public class ServiceDownException extends RuntimeException {

    public ServiceDownException(String message) {
        super(message);
    }
}