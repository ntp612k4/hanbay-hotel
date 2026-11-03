package com.tpanh.hotel.common.security;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserProvider {

    Optional<UUID> currentUserId();
}
