package com.example.user_service.repository;

import com.example.user_service.entity.LoginHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {

    @Modifying
    @Query("delete from LoginHistoryEntity loginHistory where loginHistory.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
