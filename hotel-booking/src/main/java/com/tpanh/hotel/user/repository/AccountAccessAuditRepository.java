package com.tpanh.hotel.user.repository;

import com.tpanh.hotel.user.domain.AccountAccessAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAccessAuditRepository extends JpaRepository<AccountAccessAudit, UUID> {}
