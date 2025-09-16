package com.trever.backend.api.trade.service;

import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.TransactionRepository;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final VehicleRepository vehicleRepository;
    private final ContractService contractService;

    // 일반 거래 생성
    public Transaction createTransactionFromVehicle(Long buyerId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

        Transaction tx = new Transaction();
        tx.setBuyerId(buyerId);
        tx.setSellerId(vehicle.getSellerId());
        tx.setVehicleId(vehicleId);
        tx.setFinalPrice(vehicle.getPrice());
        tx.setStatus("PENDING");

        Transaction saved = transactionRepository.save(tx);

        // 거래와 연결된 계약 자동 생성
        contractService.createContract(saved);

        return saved;
    }

    // 경매 거래 생성


    // 거래 조회
    public Transaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.TRANSACTION_NOT_FOUND.getMessage()));

    }
}
