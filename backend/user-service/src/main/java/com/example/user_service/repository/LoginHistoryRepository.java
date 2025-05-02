package com.example.user_service.repository;

import com.example.user_service.entity.LoginHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {}

