package com.trever.backend.api.user.repository;

import com.trever.backend.api.user.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByUserId(Long userId);
}