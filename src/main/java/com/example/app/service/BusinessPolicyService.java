package com.example.app.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.app.domain.BusinessPolicy;

public interface BusinessPolicyService {
    BusinessPolicy getCurrentPolicy(Integer companyId);
    boolean isPolicySet(Integer companyId);
    boolean isCurrentlyAgreed(Integer companyId);
    void setPolicy(Integer companyId, boolean isAgreed, BigDecimal markupRate,
                   String setBy, String setReason, boolean acknowledgePendingWorkUsesOldRate);
    void resetPolicyOnTransfer(Integer companyId, String operatorName);
    // 指定日の時点で有効だった設定（未設定なら null）
    BusinessPolicy getPolicyOn(Integer companyId, LocalDate date);
    // 履歴（古い順）
    List<BusinessPolicy> getPolicyHistory(Integer companyId);
}

