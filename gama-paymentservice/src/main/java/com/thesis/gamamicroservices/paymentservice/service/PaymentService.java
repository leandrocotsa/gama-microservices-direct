package com.thesis.gamamicroservices.paymentservice.service;

import com.thesis.gamamicroservices.paymentservice.dto.PaymentOrderSetDTO;
import com.thesis.gamamicroservices.paymentservice.model.PaymentOrder;
import com.thesis.gamamicroservices.paymentservice.model.PaymentStatus;
import com.thesis.gamamicroservices.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.sql.Date;
import java.util.Calendar;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    private static final String ORDER_SERVICE_CONFIRM_URL = "http://order-service:8080/orders/paymentconfirmed/";

   @Autowired
    public PaymentService(PaymentRepository paymentRepository, RestTemplate restTemplate) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
   }

    public void addPaymentToOrder(PaymentOrderSetDTO paymentOrderSetDTO, Double price) {
        PaymentOrder paymentOrder = new PaymentOrder(paymentOrderSetDTO, price);
        paymentRepository.save(paymentOrder);
    }

    public void paymentSuccessful(int orderID) {
        Optional<PaymentOrder> paymentOrder = paymentRepository.findByOrderId(orderID);
        paymentOrder.get().setPayDate(new Date(Calendar.getInstance().getTimeInMillis()));
        paymentOrder.get().setState(PaymentStatus.PAYED);
        paymentRepository.save(paymentOrder.get());

        try {
            restTemplate.put(ORDER_SERVICE_CONFIRM_URL + orderID, null); //nao testado
        } catch (HttpStatusCodeException ex) {
            System.out.println(ex.getStatusCode().toString());
            System.out.println("Order Service is currently down. Try again later");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Order Service is currently down. Try again later");
        }



        //rabbitTemplate.convertAndSend(exchange.getName(), "payment.confirmed", new PaymentCreatedMessage(paymentOrder.get()));//envia evento para a order por payed

    }

    public Optional<PaymentOrder> findPaymentByOrderId(int orderId) {
       return paymentRepository.findByOrderId(orderId);
    }

    //caso o pagamento tenha falhado este vai-se manter em pending
    //posso ter um scheduled job no orders service a ver se passaram x horas e o estado passa a expired
}
