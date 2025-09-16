package com.trever.backend.api.trade.controller;

import com.trever.backend.api.trade.dto.TransactionRequestDTO;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.service.TransactionService;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "거래 관련 API입니다.")
public class TransactionController {

    private final TransactionService transactionService;

    // 거래 생성
    @Operation(summary = "일반 거래 생성 API", description = "차량 구매 시 거래를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> createTransaction(@RequestBody TransactionRequestDTO transactionRequestDTO) {
        Transaction saved = transactionService.createTransactionFromVehicle(transactionRequestDTO.getBuyerId(), transactionRequestDTO.getVehicleId());
        return ApiResponse.success(SuccessStatus.TRANSACTION_CREATE_SUCCESS, saved);
    }

    // 경매 거래 생성
    @Operation(summary = "경매 거래 생성 API", description = "경매 종료 시 최고가 입찰자를 낙찰자로 하여 거래를 생성합니다.")
    @PostMapping("/auction/{auctionId}")
    public ResponseEntity<ApiResponse<Transaction>> createAuctionTransaction(@PathVariable Long auctionId) {
        Transaction saved = transactionService.createTransactionFromAuction(auctionId);
        return ApiResponse.success(SuccessStatus.AUCTION_TRANSACTION_CREATE_SUCCESS, saved);
    }

    // 거래 조회
    @Operation(summary = "거래 조회 API", description = "거래 ID로 거래를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(@PathVariable Long id) {
        Transaction tx = transactionService.getTransaction(id);
        return ApiResponse.success(SuccessStatus.TRANSACTION_GET_SUCCESS, tx);
    }
}
