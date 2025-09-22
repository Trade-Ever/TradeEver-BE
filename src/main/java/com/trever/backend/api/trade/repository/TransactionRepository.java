package com.trever.backend.api.trade.repository;

import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBuyerId(Long buyerId);
    List<Transaction> findBySellerId(Long sellerId);

    List<Transaction> findByBuyerIdAndStatus(Long buyerId, TransactionStatus status);
    List<Transaction> findBySellerIdAndStatus(Long sellerId, TransactionStatus status);

    // 구매자일떄, 판매자일때 진행 중인거 한 번에 보고 싶으면
    List<Transaction> findByStatusAndBuyerIdOrSellerId(TransactionStatus status, Long buyerId, Long sellerId);
}
