package com.trever.backend.api.user.service;

import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.entity.UserWallet;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.user.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserWalletService {

    private final UserWalletRepository walletRepository;
    private final UserRepository userRepository;
    
    public UserWallet getUserWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("해당 사용자의 지갑을 찾을 수 없습니다: " + userId));
    }
    
    @Transactional
    public void deposit(Long userId, Long amount) {
        if (amount <= 0) {
            throw new BadRequestException("입금액은 0보다 커야 합니다.");
        }
        
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
    }
    
    @Transactional
    public void withdraw(Long userId, Long amount) {
        if (amount <= 0) {
            throw new BadRequestException("출금액은 0보다 커야 합니다.");
        }
        
        UserWallet wallet = getUserWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new BadRequestException("잔액이 부족합니다.");
        }
        
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);
    }
    
    @Transactional
    public boolean hasSufficientFunds(Long userId, Long amount) {
        try {
            UserWallet wallet = getUserWallet(userId);
            return wallet.getBalance() >= amount;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Transactional
    public UserWallet createUserWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다: " + userId));

        UserWallet newWallet = UserWallet.builder()
                .user(user)
                .balance(0L)  // 초기 잔액 0으로 세팅
                .build();

        return walletRepository.save(newWallet);
    }

    
    private UserWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다: " + userId));
                    
                    UserWallet newWallet = UserWallet.builder()
                            .user(user)
                            .balance(0L)
                            .build();
                    
                    return walletRepository.save(newWallet);
                });
    }
}