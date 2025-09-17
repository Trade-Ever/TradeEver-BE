package com.trever.backend.api.trade.repository;

import com.trever.backend.api.trade.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
