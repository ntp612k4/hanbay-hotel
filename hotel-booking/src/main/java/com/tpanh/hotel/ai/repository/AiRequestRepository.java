package com.tpanh.hotel.ai.repository;

import com.tpanh.hotel.ai.entity.AiRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {}
