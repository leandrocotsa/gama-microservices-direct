package com.thesis.gamamicroservices.orderservice.service;

public class ServiceDownException extends RuntimeException {

    public ServiceDownException(String message) {
        super(message);
    }
}