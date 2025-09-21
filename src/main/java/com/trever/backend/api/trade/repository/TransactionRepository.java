package com.trever.backend.api.trade.repository;

import com.trever.backend.api.trade.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBuyerId(Long buyerId);
    List<Transaction> findBySellerId(Long sellerId);
}
