package com.thesis.gamamicroservices.reviewservice.dto;

import com.thesis.gamamicroservices.reviewservice.model.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReviewGetDTO {
    private int id;
    private String username;
    private int ratingStars;
    private String comment;

    public ReviewGetDTO(Review review){
        this.id = review.getId();
        this.username = review.getUserName();
        this.ratingStars = review.getRatingStars();
        this.comment = review.getComment();
    }
}

