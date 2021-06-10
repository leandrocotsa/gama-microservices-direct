package com.thesis.gamamicroservices.promotionservice.service;

public class ServiceDownException extends RuntimeException {

    public ServiceDownException(String message) {
        super(message);
    }
}