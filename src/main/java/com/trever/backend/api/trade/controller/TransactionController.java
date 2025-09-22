package com.trever.backend.api.trade.controller;

import com.trever.backend.api.trade.dto.PurchaseApplicationRequestDTO;
import com.trever.backend.api.trade.dto.PurchaseApplicationResponseDTO;
import com.trever.backend.api.trade.dto.TransactionResponseDTO;
import com.trever.backend.api.trade.entity.PurchaseApplication;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.service.TransactionService;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "거래 관련 API입니다.")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    // 구매 신청
    @Operation(summary = "구매 신청 API", description = "구매자가 차량에 구매 신청을 합니다.")
    @PostMapping("/apply/{vehicleId}")
    public ResponseEntity<ApiResponse<PurchaseApplicationResponseDTO>> apply(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername(); // JWT에서 꺼낸 email
        User buyer = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        PurchaseApplicationResponseDTO response = transactionService.apply(vehicleId, buyer.getId());
        return ApiResponse.success(SuccessStatus.PURCHASE_REQUEST_CREATE_SUCCESS, response);
    }

    // 구매 신청자 목록 조회
    @Operation(summary = "구매 신청자 목록 조회 API", description = "차량에 등록된 구매 신청자들을 조회합니다.")
    @GetMapping("/requests/{vehicleId}")
    public ResponseEntity<ApiResponse<List<PurchaseApplicationResponseDTO>>> getRequests(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        List<PurchaseApplicationResponseDTO> requests = transactionService.getRequestsByVehicle(vehicleId, seller.getId());
        return ApiResponse.success(SuccessStatus.PURCHASE_REQUEST_LIST_SUCCESS, requests);
    }

    // 판매자가 구매자 선택 → 거래 생성
    @Operation(summary = "구매자 선택 API", description = "판매자가 구매자를 선택하여 거래를 확정합니다.")
    @PostMapping("/select/{vehicleId}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> selectBuyer(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long buyerId) {

        String email = userDetails.getUsername();
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        TransactionResponseDTO response = transactionService.selectBuyer(vehicleId, seller.getId(), buyerId);
        return ApiResponse.success(SuccessStatus.TRANSACTION_CREATE_SUCCESS, response);
    }

    // 경매 거래 생성
//    @Operation(summary = "경매 거래 생성 API", description = "경매 종료 시 최고가 입찰자를 낙찰자로 하여 거래를 생성합니다.")
//    @PostMapping("/auction/{auctionId}")
//    public ResponseEntity<ApiResponse<TransactionResponseDTO>> createAuctionTransaction(@PathVariable Long auctionId) {
//        TransactionResponseDTO saved = transactionService.createTransactionFromAuction(auctionId);
//        return ApiResponse.success(SuccessStatus.AUCTION_TRANSACTION_CREATE_SUCCESS, saved);
//    }

    // 거래 조회
    @Operation(summary = "거래 조회 API", description = "거래 ID로 거래를 조회합니다.")
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> getTransaction(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        TransactionResponseDTO transactionResponseDTO = transactionService.getTransaction(transactionId, user.getId());

        return ApiResponse.success(SuccessStatus.TRANSACTION_GET_SUCCESS, transactionResponseDTO);
    }

    @Operation(summary = "구매 내역 조회 API", description = "사용자가 구매 완료한 거래 내역을 조회합니다.")
    @GetMapping("/my/purchases")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getMyPurchases(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        List<TransactionResponseDTO> purchases = transactionService.getMyPurchases(user.getId());
        return ApiResponse.success(SuccessStatus.TRANSACTION_LIST_SUCCESS, purchases);
    }

    @Operation(summary = "판매 내역 조회 API", description = "사용자가 판매 완료한 거래 내역을 조회합니다.")
    @GetMapping("/my/sales")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getMySales(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        List<TransactionResponseDTO> sales = transactionService.getMySales(user.getId());
        return ApiResponse.success(SuccessStatus.TRANSACTION_LIST_SUCCESS, sales);
    }

    @Operation(summary = "진행 중 거래 내역 (전체)", description = "내가 참여한 모든 진행 중 거래를 조회합니다.")
    @GetMapping("/my/in-progress")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getMyInProgress(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        List<TransactionResponseDTO> response = transactionService.getMyInProgress(user.getId());
        return ApiResponse.success(SuccessStatus.TRANSACTION_LIST_SUCCESS, response);
    }
}
