package com.thesis.gamamicroservices.productservice.controller;

import com.thesis.gamamicroservices.productservice.dto.PromotionEndedMessage;
import com.thesis.gamamicroservices.productservice.dto.PromotionStartedMessage;
import com.thesis.gamamicroservices.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/promotionprice")
public class PromotionPricesController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionPricesController.class);

    private static final String PROMOTION_STARTED_LOG = "Promotion started";
    private static final String PROMOTION_ENDED_LOG = "Promotion ended";

    @Autowired
    ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void promotionStarted(@RequestBody PromotionStartedMessage promotionStarted) {
        productService.setPromotionPrice(promotionStarted);
        logger.info(PROMOTION_STARTED_LOG);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void promotionEnded(@RequestBody PromotionEndedMessage promotionEndedMessage) {
        productService.resetPromotionPrice(promotionEndedMessage.getProductsEnded());
        logger.info(PROMOTION_ENDED_LOG);
    }



}
