package com.trever.backend.api.trade.repository;

import com.trever.backend.api.trade.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    // 거래 ID로 계약서 조회
    Optional<Contract> findByTransactionId(Long transactionId);
}
