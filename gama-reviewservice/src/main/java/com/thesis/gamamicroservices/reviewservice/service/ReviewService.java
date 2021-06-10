package com.thesis.gamamicroservices.reviewservice.service;

import com.thesis.gamamicroservices.reviewservice.dto.ReviewGetDTO;
import com.thesis.gamamicroservices.reviewservice.dto.ReviewSetDTO;
import com.thesis.gamamicroservices.reviewservice.model.Review;
import com.thesis.gamamicroservices.reviewservice.repository.ReviewRepository;
import com.thesis.gamamicroservices.reviewservice.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Transactional
@Service
public class ReviewService {


    private final ReviewRepository reviewRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, JwtTokenUtil jwtTokenUtil) {
        this.reviewRepository = reviewRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public void createReview(String authorizationToken, int productID, ReviewSetDTO reviewSetDTO) {
        //String email = jwtTokenUtil.getEmailFromAuthorizationString(authorizationToken);
        int userId = Integer.parseInt(jwtTokenUtil.getUserIdFromAuthorizationString(authorizationToken));
        Review r = new Review(reviewSetDTO, userId, productID);
        this.reviewRepository.save(r);
        //rabbitTemplate.convertAndSend(exchange.getName(), "review", new ReviewCreatedMessage(r));
    }


    public void deleteReview(String authorizationToken, int reviewID) throws NoDataFoundException {
        int userId = Integer.parseInt(jwtTokenUtil.getUserIdFromAuthorizationString(authorizationToken));
        Optional<Review> review = this.reviewRepository.findById(reviewID);
        if(review.isPresent() && review.get().getUserId() == userId) {
            reviewRepository.delete(review.get());
            //rabbitTemplate.convertAndSend(exchange.getName(), "review", new ReviewDeletedMessage(reviewID, review.get().getProductId()));
        }
        else {
            throw new NoDataFoundException("You cannot delete review of id " + reviewID);
        }

    }

    public void deleteReviewByProductId(int productId) {
        reviewRepository.deleteAllByProductId(productId);
    }

    public void deleteReviewByUserId(int userId) {
        reviewRepository.deleteAllByUserId(userId);
        //evento de multiple review delete, ReviewsDeletedUserDeletedMessage
    }

    public List<ReviewGetDTO> getReviewsDTOOfProduct(int productId) {
        List<ReviewGetDTO> reviews = new ArrayList<>();
        reviewRepository.findAllByProductId(productId).forEach(r -> reviews.add(new ReviewGetDTO(r)));

        return reviews;
    }
}
