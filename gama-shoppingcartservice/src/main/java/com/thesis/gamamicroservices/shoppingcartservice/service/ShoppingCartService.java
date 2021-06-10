package com.thesis.gamamicroservices.shoppingcartservice.service;

import com.thesis.gamamicroservices.shoppingcartservice.dto.ShoppingCartItemSetDTO;
import com.thesis.gamamicroservices.shoppingcartservice.model.ShoppingCart;
import com.thesis.gamamicroservices.shoppingcartservice.model.ShoppingCartItem;
import com.thesis.gamamicroservices.shoppingcartservice.repository.ShoppingCartRepository;
import com.thesis.gamamicroservices.shoppingcartservice.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Transactional
@Service
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository, JwtTokenUtil jwtTokenUtil) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public void createShoppingCartForUser(int userId) {
        ShoppingCart s = new ShoppingCart(userId);
        shoppingCartRepository.save(s);
    }

    public void addItemToCart(String authorizationToken, ShoppingCartItemSetDTO itemSetDTO) {
        int userId = Integer.parseInt(jwtTokenUtil.getUserIdFromAuthorizationString(authorizationToken));
        ShoppingCart shoppingCart = shoppingCartRepository.findByUserId(userId);
        shoppingCart.addShoppingCartItem(new ShoppingCartItem(itemSetDTO));
        shoppingCartRepository.save(shoppingCart);

        //comentei porque a view e recommendation service ainda nao existem e ia ficar na fila forever
        //String productJson = objectWriter.writeValueAsString(new CartItemAddedDTO(userId, itemSetDTO.getProductId(), itemSetDTO.getQty()));

        //rabbitTemplate.convertAndSend(exchange.getName(), RoutingKeys.ADDED.getNotation(), productJson);
    }

    public void removeItemFromCart(String authorizationToken, int itemID) {
        int userId = Integer.parseInt(jwtTokenUtil.getUserIdFromAuthorizationString(authorizationToken));
        ShoppingCart shoppingCart = shoppingCartRepository.findByUserId(userId);
        shoppingCart.removeCartItem(itemID);
        shoppingCartRepository.save(shoppingCart);

        //EVENTO ITEM REMOVED PARA A VIEW E RECOMMENDATION SERVICE
    }

    //update de quantidades meh, a pessoa que apague o item e adicione outra vez com a quantidade certa

    public void cleanShoppingCart(int userId) {
        ShoppingCart shoppingCart = shoppingCartRepository.findByUserId(userId);
        shoppingCart.cleanCart();
        shoppingCartRepository.save(shoppingCart);
    }

    //scheduled job para expirar em 24horas?


}
