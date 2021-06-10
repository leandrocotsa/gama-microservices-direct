package com.thesis.gamamicroservices.promotionservice.service;


import com.thesis.gamamicroservices.promotionservice.dto.ProductReferenceSetDTO;
import com.thesis.gamamicroservices.promotionservice.dto.PromotionSetDTO;
import com.thesis.gamamicroservices.promotionservice.dto.messages.*;
import com.thesis.gamamicroservices.promotionservice.model.Promotion;
import com.thesis.gamamicroservices.promotionservice.model.PromotionState;
import com.thesis.gamamicroservices.promotionservice.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Service
public class PromotionService {

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8080/promotionprice";

    private final PromotionRepository promotionRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public PromotionService (PromotionRepository promotionRepository, RestTemplate restTemplate){
        this.promotionRepository = promotionRepository;
        this.restTemplate = restTemplate;
    }


    public Promotion getPromotionById(int id) throws NoDataFoundException {
        Optional<Promotion> promotion = this.promotionRepository.findById(id);
        if (promotion.isPresent()) {
            return promotion.get();
        } else {
            throw new NoDataFoundException("There's no promotion with id " + id);
        }
    }


    public void createPromotion(PromotionSetDTO promotionSetDTO) throws PromotionConflictException {

        Promotion promotion = new Promotion(promotionSetDTO);
        List<Integer> products = new ArrayList<>();

        for (Integer pID : promotionSetDTO.getProductsIDs()) {
            if(!isProductAlreadyInAPromotion(pID)) {
                products.add(pID);
            } else {
                throw new PromotionConflictException("Product " + pID + " is already associated with a promotion");
            }
        }
        promotion.setProductsIds(products);
        promotionRepository.save(promotion);
        //rabbitTemplate.convertAndSend(promotionExchange.getName(), "promotion", new PromotionCreatedMessage(promotion));

        //ugh isto vai ter de ser uma mensagem tambem para a view ter as promotions
    }

    //so permito apagar uma promoção se ela nao estiver ativa
    //se quiser apagar uma promoção ativa tenho que a terminar primeiro
    //assim evito ter tambem de propagar eliminação de pormoções que ate ja podem ter acabado ou nem começado
    public void deletePromotion(int promotionID) throws NoDataFoundException, ServiceDownException {
        Promotion p = this.getPromotionById(promotionID);
        promotionRepository.delete(p);
        if(p.getState().equals(PromotionState.ACTIVE)) {
            //rabbitTemplate.convertAndSend(priceExchange.getName(), "promotion", new PromotionEndedMessage(p.getProductsIds()));
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                HttpEntity<PromotionEndedMessage> entity = new HttpEntity<PromotionEndedMessage>(new PromotionEndedMessage(p.getProductsIds()),headers);

                restTemplate.exchange(PRODUCT_SERVICE_URL, HttpMethod.DELETE, entity, String.class);
            } catch (HttpStatusCodeException ex) {
                System.out.println(ex.getStatusCode().toString());
                throw new ServiceDownException("Product Service is currently down. Try again later");
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceDownException("Product Service is currently down. Try again later");
            }
        }
        //rabbitTemplate.convertAndSend(promotionExchange.getName(), "promotion", new PromotionDeletedMessage(promotionID));
    }


    //edit promotion (value, name and description)

    public void addProductsToPromotion(int promotionID, ProductReferenceSetDTO productReferenceSetDTO) throws NoDataFoundException, PromotionConflictException, ServiceDownException {
        Promotion promotion = this.getPromotionById(promotionID);
        List<Integer> newProducts = new ArrayList<>();

        for (Integer pID : productReferenceSetDTO.getProductsIDs()) {
            if(!isProductAlreadyInAPromotion(pID)) {
                newProducts.add(pID);
            } else {
                throw new PromotionConflictException("Product " + pID + " is already associated with a promotion");
            }
        }
        List<Integer> allProducts = Stream.concat(promotion.getProductsIds().stream(), newProducts.stream())
                .collect(Collectors.toList());
        promotion.setProductsIds(allProducts);

        promotionRepository.save(promotion);

        if(promotion.getState().equals(PromotionState.ACTIVE)) {
            //rabbitTemplate.convertAndSend(priceExchange.getName(), "promotion", new PromotionStartedMessage(newProducts, promotion.getDiscountAmount(), promotion.getId()));
            try {
                restTemplate.postForObject(PRODUCT_SERVICE_URL, new PromotionStartedMessage(newProducts, promotion.getDiscountAmount(), promotion.getId()), PromotionStartedMessage.class);
            } catch (HttpStatusCodeException ex) {
                System.out.println(ex.getStatusCode().toString());
                throw new ServiceDownException("Product Service is currently down. Try again later");
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceDownException("Product Service is currently down. Try again later");
            }
        }

        //rabbitTemplate.convertAndSend(promotionExchange.getName(), "promotion", new PromotionUpdatedMessage(allProducts, promotionID));

    }

    private boolean isProductAlreadyInAPromotion(int productId) {
        Optional<Promotion> matchingObject = promotionRepository.findActiveOrScheduledPromotions().stream().
                filter(p -> p.getProductsIds().contains(productId)).
                findFirst();

        return matchingObject.isPresent();
    }

    public void removeProductFromPromotion(int promotionID, ProductReferenceSetDTO productReferenceSetDTO) throws NoDataFoundException, ServiceDownException {

        Promotion promotion = this.getPromotionById(promotionID);
        List<Integer> removedProducts = new ArrayList<>(productReferenceSetDTO.getProductsIDs());

        for (Integer pID : productReferenceSetDTO.getProductsIDs()) {
            if(promotion.getProductsIds().contains(pID)) {
                promotion.getProductsIds().remove(pID);
            } else {
                removedProducts.remove(pID);
            }
        }
        promotionRepository.save(promotion);
        if(promotion.getState().equals(PromotionState.ACTIVE)) {
            //rabbitTemplate.convertAndSend(priceExchange.getName(), "promotion", new PromotionEndedMessage(removedProducts));
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                HttpEntity<PromotionEndedMessage> entity = new HttpEntity<PromotionEndedMessage>(new PromotionEndedMessage(removedProducts),headers);

                restTemplate.exchange(PRODUCT_SERVICE_URL, HttpMethod.DELETE, entity, String.class);

            } catch (HttpStatusCodeException ex) {
                System.out.println(ex.getStatusCode().toString());
                throw new ServiceDownException("Product Service is currently down. Try again later");
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceDownException("Product Service is currently down. Try again later");
            }
        }

        //rabbitTemplate.convertAndSend(promotionExchange.getName(), "promotion", new PromotionUpdatedMessage(promotion.getProductsIds(), promotionID));

        //SE A PROMOÇÃO JA ESTIVER EM ACTIVE ENTAO MANDO EVENTO, SENAO NAO
    }

    //triggered by a scheduled job?
    public void endPromotion(int promotionID) throws NoDataFoundException, ServiceDownException {
        Promotion p = this.getPromotionById(promotionID);
        p.setState(PromotionState.EXPIRED);
        ArrayList<Integer> products = new ArrayList<>(p.getProductsIds());
        this.promotionRepository.save(p);
        //rabbitTemplate.convertAndSend(priceExchange.getName(), "promotion", new PromotionEndedMessage(products));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<PromotionEndedMessage> entity = new HttpEntity<PromotionEndedMessage>(new PromotionEndedMessage(products),headers);

            restTemplate.exchange(PRODUCT_SERVICE_URL, HttpMethod.DELETE, entity, String.class);

        } catch (HttpStatusCodeException ex) {
            System.out.println(ex.getStatusCode().toString());
            throw new ServiceDownException("Product Service is currently down. Try again later");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceDownException("Product Service is currently down. Try again later");
        }

        //ENVIAR EVENTO COM LISTA DE IDS PARA O PRODUCT SERVICE MUDAR OS PREÇOS
    }

    //triggered by a scheduled job?
    public void startPromotion(int promotionID) throws NoDataFoundException, ServiceDownException {
        Promotion p = this.getPromotionById(promotionID);
        p.setState(PromotionState.ACTIVE);
        this.promotionRepository.save(p);
        //rabbitTemplate.convertAndSend(priceExchange.getName(), "promotion", new PromotionStartedMessage(p));

        try {
            restTemplate.postForObject(PRODUCT_SERVICE_URL, new PromotionStartedMessage(p), PromotionStartedMessage.class);
        } catch (HttpStatusCodeException ex) {
            System.out.println(ex.getStatusCode().toString());
            throw new ServiceDownException("Product Service is currently down. Try again later");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceDownException("Product Service is currently down. Try again later");
        }


        /**
        try {
            String productsJson = objectWriter.writeValueAsString(new PromotionStartedMessage(p));
            rabbitTemplate.convertAndSend(exchange.getName(), "promotion.started", productsJson);
        } catch (JsonProcessingException e){
            e.printStackTrace();
        }
         **/
    }

    public void updatePromotion(Map<String, Object> updates, int promotion_id) throws NoDataFoundException, EditActivePromotionException {
        Promotion promotion = this.getPromotionById(promotion_id);
        if(!promotion.getState().equals(PromotionState.ACTIVE)) {
            try {
                // Map key is field name, v is value
                updates.forEach((k, v) -> {
                    // use reflection to get field k on manager and set it to value v
                    Field field = ReflectionUtils.findField(Promotion.class, k);
                    field.setAccessible(true);
                    ReflectionUtils.setField(field, promotion, v);
                });
                promotionRepository.save(promotion);
            } catch (Exception e) {
                e.printStackTrace();
                throw new NoDataFoundException("Invalid arguments");
            }
        } else {
            throw new EditActivePromotionException("You cannot update an active promotion");
        }
    }

}
