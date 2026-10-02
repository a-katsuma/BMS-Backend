package com.example.app.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.domain.BusinessPolicy;
import com.example.app.mapper.BusinessPolicyMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BusinessPolicyServiceImpl implements BusinessPolicyService {

    private final BusinessPolicyMapper policyMapper;

    @Override
    public BusinessPolicy getCurrentPolicy(Integer companyId) {
        return policyMapper.selectCurrentByCompanyId(companyId);
    }

    @Override
    public boolean isPolicySet(Integer companyId) {
        BusinessPolicy current = policyMapper.selectCurrentByCompanyId(companyId);
        return current != null && !"TRANSFER".equals(current.getSetReason());
    }

    @Override
    public boolean isCurrentlyAgreed(Integer companyId) {
        BusinessPolicy current = policyMapper.selectCurrentByCompanyId(companyId);
        return current != null && current.getIsAgreed() == 1;
    }

    @Override
    public void setPolicy(Integer companyId, boolean isAgreed, BigDecimal markupRate,
                           String setBy, String setReason, boolean acknowledgePendingWorkUsesOldRate) {

        BusinessPolicy current = policyMapper.selectCurrentByCompanyId(companyId);

        if ("CHANGE".equals(setReason) && current != null && current.getIsAgreed() == 1
                && !acknowledgePendingWorkUsesOldRate) {
            throw new IllegalStateException("変更前に受注済みの取引には、旧割合が適用されることへの同意が必要です");
        }

        if (isAgreed && markupRate == null) {
            throw new IllegalArgumentException("同意する場合は加算割合の入力が必要です");
        }

        if (current != null) {
            policyMapper.closeCurrentPolicy(current.getPolicyId());
        }

        policyMapper.insert(BusinessPolicy.builder()
                .companyId(companyId)
                .isAgreed(isAgreed ? 1 : 0)
                .markupRate(isAgreed ? markupRate : null)
                .effectiveFrom(LocalDateTime.now())
                .setBy(setBy)
                .setReason(setReason)
                .build());
    }

    @Override
    public void resetPolicyOnTransfer(Integer companyId, String operatorName) {
        BusinessPolicy current = policyMapper.selectCurrentByCompanyId(companyId);
        if (current != null) {
            policyMapper.closeCurrentPolicy(current.getPolicyId());
        }
        policyMapper.insert(BusinessPolicy.builder()
                .companyId(companyId)
                .isAgreed(0)
                .markupRate(null)
                .effectiveFrom(LocalDateTime.now())
                .setBy(operatorName)
                .setReason("TRANSFER")
                .build());
    }

    @Override
    public BusinessPolicy getPolicyOn(Integer companyId, LocalDate date) {
        return policyMapper.selectEffectiveOn(companyId, date);
    }

    @Override
    public List<BusinessPolicy> getPolicyHistory(Integer companyId) {
        return policyMapper.selectHistoryByCompanyId(companyId);
    }

}