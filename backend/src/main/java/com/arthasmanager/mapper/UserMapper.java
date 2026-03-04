package com.arthasmanager.mapper;

import com.arthasmanager.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User findByUsername(String username);
    User findById(Long id);
    void insert(User user);
}
