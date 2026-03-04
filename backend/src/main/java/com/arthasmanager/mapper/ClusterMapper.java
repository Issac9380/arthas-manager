package com.arthasmanager.mapper;

import com.arthasmanager.entity.ClusterEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClusterMapper {
    List<ClusterEntity> findByUserId(Long userId);
    ClusterEntity findById(String id);
    ClusterEntity findDefaultByUserId(Long userId);
    void insert(ClusterEntity cluster);
    void deleteById(String id);
    void updateStatus(@Param("id") String id, @Param("status") String status,
                      @Param("statusMessage") String statusMessage);
}
