package com.thesis.gamamicroservices.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReviewGetDTO {
    private int id;
    private String username;
    private int ratingStars;
    private String comment;
}

