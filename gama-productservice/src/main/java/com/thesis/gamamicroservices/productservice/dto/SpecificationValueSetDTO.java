package com.thesis.gamamicroservices.productservice.dto;

import com.thesis.gamamicroservices.productservice.model.SpecificationValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpecificationValueSetDTO {
    private String specificationName;
    private String specificationValue;

    public SpecificationValueSetDTO(SpecificationValue specificationValue){
        this.specificationName = specificationValue.getSpecificationName();
        this.specificationValue = specificationValue.getValue();
    }

}
