package com.thesis.gamamicroservices.orderservice.service;

import com.thesis.gamamicroservices.orderservice.dto.*;
import com.thesis.gamamicroservices.orderservice.model.Order;
import com.thesis.gamamicroservices.orderservice.model.OrderItem;
import com.thesis.gamamicroservices.orderservice.model.OrderStatus;
import com.thesis.gamamicroservices.orderservice.model.Shipping;
import com.thesis.gamamicroservices.orderservice.repository.OrderRepository;
import com.thesis.gamamicroservices.orderservice.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.util.*;

@Transactional
@Service
public class OrderService {

    private static final String PRODUCT_SERVICE_URL = "http://product-service:8080/products/list";
    private static final String INVENTORY_CHECK_URL = "http://inventory-service:8080/inventories/check";

    private final OrderRepository orderRepository;
    private final ShippingService shippingService;
    private final JwtTokenUtil jwtTokenUtil;
    private final RestTemplate restTemplate;

    @Autowired
    public OrderService(RestTemplate restTemplate, OrderRepository orderRepository, ShippingService shippingService, JwtTokenUtil jwtTokenUtil) {
        this.orderRepository = orderRepository;
        this.shippingService = shippingService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.restTemplate = restTemplate;
    }


    public Order getOrderById(int id) throws NoDataFoundException { //maybe verificar o user com o authroization token aqui?
        Optional<Order> order = this.orderRepository.findById(id);
        if(order.isPresent()) {
            return order.get();
        }
        else {
            throw new NoDataFoundException("There's no order with id " + id);
        }
    }

    public void deleteOrder(String authorizationToken, int id) throws NoDataFoundException {
        if(orderRepository.existsById(id)) { //evita que tenha de fazer um fetch extra
            orderRepository.deleteById(id);
        } else {
            throw new NoDataFoundException ("There's no Warehouse with that id");
        }
    }

    public void createOrder(OrderSetDTO orderSetDTO, String authorizationToken) throws NoDataFoundException {
        String email = jwtTokenUtil.getEmailFromAuthorizationString(authorizationToken);
        int userId = Integer.parseInt(jwtTokenUtil.getUserIdFromAuthorizationString(authorizationToken));
        Order newOrder = new Order(orderSetDTO, userId, email);

        List<OrderItem> orderItems = new ArrayList<>();
        //tenho que obter os preços de uma lista produtos do product service

        String productsIds = "";
        for(OrderItemSetDTO orderItemDTO : orderSetDTO.getOrderItems()){
            if (productsIds.length() < 1) {
                productsIds = productsIds.concat(String.valueOf(orderItemDTO.getProductId()));
            } else {
                productsIds = productsIds.concat("," + orderItemDTO.getProductId());
            }
        }

        String finalURL = PRODUCT_SERVICE_URL + "/" + productsIds;
        System.out.println(finalURL);

        List<ProductForOrderGetDTO> products;

        try {
            ResponseEntity<List<ProductForOrderGetDTO>> response =
                    restTemplate.exchange(finalURL,
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<ProductForOrderGetDTO>>() {
                            });
            products = response.getBody();
        } catch (HttpStatusCodeException ex) {
            if(ex.getRawStatusCode() == 404) {
                throw new NoDataFoundException("There is no product with that ID");
            } else {
                System.out.println(ex.getStatusCode().toString());
                throw new ServiceDownException("Product Service is currently down. Try again later");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceDownException("Product Service is currently down. Try again later");
        }

        //ProductForOrderGetDTO[] products = restTemplate.getForObject(finalURL, ProductForOrderGetDTO[].class);

        Map<Integer,Integer> products_qty = new HashMap<>();

        for(OrderItemSetDTO orderItemDTO : orderSetDTO.getOrderItems()){
            ProductForOrderGetDTO product = products.stream().filter(p -> p.getProductId()==orderItemDTO.getProductId()).findFirst().orElse(null);
            orderItems.add(new OrderItem(orderItemDTO, product));
            products_qty.put(orderItemDTO.getProductId(), orderItemDTO.getQty());
        }

        newOrder.setAllOrderItems(orderItems);
        Shipping shipping = new Shipping(shippingService.calculateShippingValue(newOrder.getTotalWeight(), orderSetDTO.getCountry()), "notes", orderSetDTO.getAddress(), orderSetDTO.getCountry());
        newOrder.addShippingToOrder(shipping);
        orderRepository.save(newOrder);

        Boolean hasStock = false;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<Map<Integer,Integer>> entity = new HttpEntity<Map<Integer,Integer>>(products_qty,headers);

            hasStock = restTemplate.exchange(INVENTORY_CHECK_URL, HttpMethod.PUT, entity, Boolean.class).getBody();

        } catch (HttpStatusCodeException ex) {
            System.out.println(ex.getStatusCode().toString());
            throw new ServiceDownException("Inventory Service is currently down. Try again later");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceDownException("Inventory Service is currently down. Try again later");
        }

        if(hasStock) {
            newOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(newOrder);
            //rabbitTemplate.convertAndSend(ordersExchange.getName(), RoutingKeys.ORDER_CONFIRMED.getNotation(), new OrderConfirmedMessage(o));
        } else {
            newOrder.setOrderStatus(OrderStatus.REJECTED);
            orderRepository.save(newOrder);
            //rabbitTemplate.convertAndSend(ordersExchange.getName(), RoutingKeys.ORDER_UPDATED.getNotation(), new OrderStatusUpdateMessage(o));
        }
    }


/**

    public void processStock(StockCheckMessage stockCheckMessage) {
        Order o;
        try {
            o = getOrderById(stockCheckMessage.getOrderId());
        } catch (NoDataFoundException e) {
            e.printStackTrace();
            return;
        }
        if(stockCheckMessage.isStockAvailable()) {
            o.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(o);
            //rabbitTemplate.convertAndSend(ordersExchange.getName(), RoutingKeys.ORDER_CONFIRMED.getNotation(), new OrderConfirmedMessage(o));
        } else {
            o.setOrderStatus(OrderStatus.REJECTED);
            orderRepository.save(o);
            //rabbitTemplate.convertAndSend(ordersExchange.getName(), RoutingKeys.ORDER_UPDATED.getNotation(), new OrderStatusUpdateMessage(o));
        }

    }
 **/


    public void paymentConfirmed(int orderId) throws NoDataFoundException {
        Order order = getOrderById(orderId);
        order.setOrderStatus(OrderStatus.APPROVED);
        orderRepository.save(order);
        //rabbitTemplate.convertAndSend(ordersExchange.getName(), RoutingKeys.ORDER_UPDATED.getNotation(), new OrderStatusUpdateMessage(order));
    }


    //scheduled job calls this method if order hasnt been approved for 24 horus
    public void expireOrder(int orderId) throws NoDataFoundException {
        Order order = getOrderById(orderId);
        order.setOrderStatus(OrderStatus.EXPIRED);

    }


    public Double getTotalOfOrder(int orderId) throws NoDataFoundException {
        Order o = this.getOrderById(orderId);
        return o.calculateTotalValueToPay();
    }
}
