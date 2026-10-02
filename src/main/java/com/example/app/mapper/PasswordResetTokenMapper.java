package com.example.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.domain.PasswordResetToken;

@Mapper
public interface PasswordResetTokenMapper {
    void insert(PasswordResetToken token);
    PasswordResetToken selectByToken(@Param("token") String token);
    void markUsed(@Param("token") String token);
    void invalidateAllForUser(@Param("userId") Integer userId);
}