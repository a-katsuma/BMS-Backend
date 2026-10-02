package com.example.app.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessPolicy {
    private Integer policyId;
    private Integer companyId;
    private Integer isAgreed;
    private BigDecimal markupRate;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String setBy;
    private String setReason;
    private LocalDateTime createdAt;
}