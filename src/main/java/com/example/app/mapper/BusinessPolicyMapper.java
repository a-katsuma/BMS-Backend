package com.example.app.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.BusinessPolicy;

@Mapper
public interface BusinessPolicyMapper {
	BusinessPolicy selectCurrentByCompanyId(@Param("companyId") Integer companyId);

	void insert(BusinessPolicy policy);

	void closeCurrentPolicy(@Param("policyId") Integer policyId);

	BusinessPolicy selectEffectiveOn(@Param("companyId") Integer companyId, @Param("date") LocalDate date);

	List<BusinessPolicy> selectHistoryByCompanyId(@Param("companyId") Integer companyId);
}
