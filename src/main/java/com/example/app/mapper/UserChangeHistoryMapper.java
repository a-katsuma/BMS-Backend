package com.example.app.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.app.domain.UserChangeHistory;

@Mapper
public interface UserChangeHistoryMapper {
    void insert(UserChangeHistory history);
}