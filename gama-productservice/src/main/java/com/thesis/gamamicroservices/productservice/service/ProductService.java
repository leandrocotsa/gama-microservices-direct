package com.thesis.gamamicroservices.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thesis.gamamicroservices.productservice.dto.*;
import com.thesis.gamamicroservices.productservice.model.*;
import com.thesis.gamamicroservices.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.*;

@Transactional
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final RestTemplate restTemplate;

    private static final String PRODUCT_NOT_FOUND = "There is no product with id: ";
    private static final String PRODUCT_DELETE_INVENTORY_URL = "http://localhost:8081/inventories/products/";
    private static final String PRODUCT_DELETE_REVIEW_URL = "http://localhost:8085/reviews/products/";
    private static final String GET_REVIEWS_URL = "http://localhost:8085/reviews/products/";
    private static final String GET_INVENTORIES_URL = "http://localhost:8081/inventories/products/";

   @Autowired
    public ProductService(ProductRepository productRepository, BrandService brandService, CategoryService categoryService, RestTemplate restTemplate) {
        this.productRepository = productRepository;
        this.brandService = brandService;
        this.categoryService = categoryService;
        this.restTemplate = restTemplate;
    }


    private Product getProductById(int id) throws NoDataFoundException {

        return productRepository.findById(id)
                .orElseThrow(() -> new NoDataFoundException(PRODUCT_NOT_FOUND + id));

        /**
        Optional<Product> product = this.productRepository.findById(id);
        if(product.isPresent()) {
            return product.get();
        }
        else {
            throw new NoDataFoundException(PRODUCT_NOT_FOUND + id);
        }
         **/
    }


    public void deleteProduct(int id) throws NoDataFoundException {
        if(productRepository.existsById(id)) {
            productRepository.deleteById(id);

            try {
                restTemplate.delete(PRODUCT_DELETE_INVENTORY_URL + id);
                restTemplate.delete(PRODUCT_DELETE_REVIEW_URL + id);
            }catch (HttpStatusCodeException ex) {
                System.out.println(ex.getStatusCode().toString());
                throw new ServiceDownException("Inventory or Review Service is currently down. Try again later");
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceDownException("Inventory or Review Service is currently down. Try again later");
            }
            //rabbitTemplate.convertAndSend(deletedCreatedExchange.getName(), RoutingKeys.PRODUCT_DELETED.getNotation(), new ProductDeletedMessage(id));
        }
        else {
            throw new NoDataFoundException(PRODUCT_NOT_FOUND + id);
        }
    }

    public void createProduct(ProductSetDTO productSetDTO) throws NoDataFoundException, JsonProcessingException {
        //hmm verifico se nome do produto já existe?
        //Optional<Product> existingProduct = this.carRepository.findByLicensePlate(carSetDto.getLicensePlate());

        Brand brand = this.brandService.findById(productSetDTO.getBrandId());
        Category category = this.categoryService.findById(productSetDTO.getCategoryId());
        List<SpecificationValue> specificationValues = new ArrayList<>();

        for(SpecificationValueSetDTO s : productSetDTO.getSpecificationValues()) {
            //Specification specification = specificationService.findById(s.getSpecificationId());
            specificationValues.add(new SpecificationValue(s));
        }

        Product p = new Product(productSetDTO, brand, category);
        p.addSpecificationValuesToProduct(specificationValues);
        this.productRepository.save(p);

        //posso enviar product com as specs todas para a view e nos serviços que nao a view as classes que recebem nao têm a info que nao precisam e nao dá erro
        //String productJson = objectWriter.writeValueAsString(new ProductCreatedDTO(p, brand.getId(), category.getId()));

        //rabbitTemplate.convertAndSend(deletedCreatedExchange.getName(), RoutingKeys.PRODUCT_CREATED.getNotation(), new ProductCreatedMessage(p, brand.getId(), brand.getName(), category.getId(), category.getName()));
        //ver rabbittamplate.receiveandconvert

    }


    //nao funciona para mudar nested entities como category e brand, so muda primitivas
    //posso analisar o map que vem e verificar se tem category e brand e faço set manualmente desses
    //ao modificar o preço tambem, tenho que ter em atenção promotion price que é sempre em relação ao price
    public void editProduct(Map<String, Object> updates, int productId) throws NoDataFoundException {
        Map<String, Object> duplicated_updates = new HashMap<>(updates);
        Product product = this.getProductById(productId);
        Double productOldPrice = product.getPrice();
            try {
                // Map key is field name, v is value
                updates.forEach((k, v) -> {
                    // use reflection to get field k on manager and set it to value v
                    try {
                        Field field = ReflectionUtils.findField(Product.class, k);
                        field.setAccessible(true);
                        ReflectionUtils.setField(field, product, v);
                    } catch (NullPointerException e) {
                        if(!(k.equals("brandId") || k.equals("categoryId"))) {
                            throw new NullPointerException();
                        }
                    }


                    switch (k) {
                        case "price":
                            if (product.getPromotionPrice() != null) {
                                int discountAmount = (int) ((product.getPromotionPrice() * 100) / productOldPrice);
                                Double newPromotionPrice = product.getPrice() * discountAmount;
                                product.setPromotionPrice(newPromotionPrice);
                                duplicated_updates.put("promotionPrice", newPromotionPrice);
                            }
                            break;
                        case "brandId":
                            try {
                                Brand brand = brandService.findById((Integer) v);
                                product.setBrand(brand);
                                duplicated_updates.put("brandName", brand.getName());
                            } catch (NoDataFoundException e) {
                                duplicated_updates.remove(k);
                                throw new NullPointerException();
                            }
                            break;
                        case "categoryId":
                            try {
                                Category category = categoryService.findById((Integer) v);
                                product.setCategory(category);
                                duplicated_updates.put("categoryName", category.getName());
                            } catch (NoDataFoundException e) {
                                duplicated_updates.remove(k);
                                throw new NullPointerException();
                            }
                            break;
                    }
                });
                productRepository.save(product);
                duplicated_updates.put("id", productId);
                //ProductUpdatedMessage productUpdated = new ProductUpdatedMessage(duplicated_updates);
                //rabbitTemplate.convertAndSend(updatedExchange.getName(), RoutingKeys.PRODUCT_UPDATED.getNotation(), productUpdated);
                //EVENTO PARA A VIEW DE PRODUCT UPDATE
            } catch(Exception e) {
                e.printStackTrace();
                throw new NoDataFoundException ("Invalid arguments");
            }

        //evento que só interessa à view
        //so as alterações de preço interessam a outros serviços

    }

    //passa a ser um pedido REST
    public void setPromotionPrice(PromotionStartedMessage promotionStarted) {
        //ArrayList<Integer> productsId = new ArrayList<>();
        Map<Integer,Double> products_price = new HashMap<>();
        for(int productId : promotionStarted.getProductsIds()) {
            try {
                Product product = this.getProductById(productId);
                Double newPrice = product.getPrice() - (product.getPrice() * (promotionStarted.getDiscountAmount()) / 100);
                product.setPromotionPrice(newPrice);
                productRepository.save(product);
                products_price.put(productId, newPrice);
                //productsId.add(productId);
            } catch (NoDataFoundException e) {
                e.printStackTrace();
            }
        }
        //rabbitTemplate.convertAndSend(updatedExchange.getName(), RoutingKeys.PROMOTION_STARTED.getNotation(), new PromotionPriceMessage(products_price, promotionStarted.getPromotionId()));

        //promotionStarted.setProductsIds(productsId);
/**
        try {
            String productsJson = objectWriter.writeValueAsString(new PromotionPriceMessage(products_price));
            rabbitTemplate.convertAndSend(updatedExchange.getName(), RoutingKeys.PROMOTION_STARTED.getNotation(), productsJson);
        } catch (JsonProcessingException e){
            e.printStackTrace();
        }

**/
    }

    //passa a ser um pedido REST
    public void resetPromotionPrice(List<Integer> productsEnded) {
        for(int pId : productsEnded) {
            try {
                Product product = this.getProductById(pId);
                product.setPromotionPrice(null);
                productRepository.save(product);
            } catch (NoDataFoundException e) {
                e.printStackTrace();
            }
        }
        //ArrayList<Integer> products = new ArrayList<>(productsEnded);
        //rabbitTemplate.convertAndSend(updatedExchange.getName(), RoutingKeys.PROMOTION_ENDED.getNotation(), new PromotionPriceResetMessage(products));

    }

    //quando era só um produto
    public void resetPromotionPrice(int productId) {
        try {
            Product product = this.getProductById(productId);
            product.setPromotionPrice(null);
            productRepository.save(product);
        } catch (NoDataFoundException e) {
            e.printStackTrace();
        }
    }


    public List<ProductForOrderGetDTO> getListOfProducts(List<Integer> productsIds) throws NoDataFoundException {
        List<ProductForOrderGetDTO> productList = new ArrayList<>();
       for(Integer id : productsIds) {
           Product p = this.getProductById(id);
           productList.add(new ProductForOrderGetDTO(p));
       }
       return productList;
    }

    public Boolean productExists(Integer productId) {
       return productRepository.existsById(productId);
    }

    public ProductGetDTO getProductDetailsById(int id) throws NoDataFoundException {
        Product product = this.getProductById(id);

        List<ReviewGetDTO> reviews;
        List<InventoryGetDTO> inventories;

        try {
            ResponseEntity<List<ReviewGetDTO>> responseReviews =
                    restTemplate.exchange(GET_REVIEWS_URL + id,
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<ReviewGetDTO>>() {
                            });
            reviews = responseReviews.getBody();

            ResponseEntity<List<InventoryGetDTO>> responseInventories =
                    restTemplate.exchange(GET_INVENTORIES_URL + id,
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<InventoryGetDTO>>() {
                            });
            inventories = responseInventories.getBody();

        }  catch (Exception e) {
            e.printStackTrace();
            System.out.println("Review or Order Service are currently down.");
            return new ProductGetDTO(product);
        }

        return new ProductGetDTO(product, reviews, inventories);
    }
}
