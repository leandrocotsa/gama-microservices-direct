package com.thesis.gamamicroservices.productservice.service;

public class ServiceDownException extends RuntimeException {

    public ServiceDownException(String message) {
        super(message);
    }
}