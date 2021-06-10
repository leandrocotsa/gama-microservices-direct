package com.thesis.gamamicroservices.paymentservice.service;

public class ServiceDownException extends RuntimeException {

    public ServiceDownException(String message) {
        super(message);
    }
}