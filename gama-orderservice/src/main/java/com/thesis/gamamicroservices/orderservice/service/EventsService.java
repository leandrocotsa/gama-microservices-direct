package com.thesis.gamamicroservices.orderservice.service;
/**
import com.thesis.gamamicroservices.orderservice.repository.ProductReplicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class EventsService {

    private final ProductReplicaRepository productRepository;
    private final OrderService orderService;

    @Autowired
    public EventsService(ProductReplicaRepository productRepository, OrderService orderService) {
        this.productRepository = productRepository;
        this.orderService = orderService;
    }


    public void paymentConfirmed(int orderId) {
        try {
            orderService.paymentConfirmed(orderId);
        } catch (NoDataFoundException e) {
            e.printStackTrace();
        }
    }

}
**/