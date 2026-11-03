package com.tpanh.hotel.authentication.repository;

import com.tpanh.hotel.authentication.domain.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("select token from PasswordResetToken token join fetch token.user where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token join fetch token.user where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update PasswordResetToken token
            set token.revokedAt = :revokedAt
            where token.user.id = :userId
              and token.usedAt is null
              and token.revokedAt is null
            """)
    int revokeActiveTokensByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update PasswordResetToken token
            set token.revokedAt = :revokedAt
            where token.user.id = :userId
              and token.id <> :currentTokenId
              and token.usedAt is null
              and token.revokedAt is null
            """)
    int revokeOtherActiveTokensByUserId(
            @Param("userId") UUID userId,
            @Param("currentTokenId") UUID currentTokenId,
            @Param("revokedAt") Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update PasswordResetToken token
            set token.revokedAt = :revokedAt
            where token.tokenHash = :tokenHash
              and token.usedAt is null
              and token.revokedAt is null
            """)
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PasswordResetToken token where token.expiredAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
