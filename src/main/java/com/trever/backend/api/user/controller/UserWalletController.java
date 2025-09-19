package com.trever.backend.api.user.controller;

import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.user.service.UserWalletService;
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

@Tag(name = "Wallet", description = "지갑 관련 API")
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class UserWalletController {

    private final UserWalletService userWalletService;
    private final UserRepository userRepository;

    @Operation(summary = "지갑 잔액 조회", description = "특정 유저의 지갑 잔액을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Long>> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        Long balance = userWalletService.getUserWallet(user.getId()).getBalance();
        return ApiResponse.success(SuccessStatus.GET_BALANCE_SUCCESS, balance);
    }

    @Operation(summary = "지갑 충전", description = "특정 유저의 지갑에 금액을 충전합니다.")
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Void>> deposit(@AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestParam Long amount) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        userWalletService.deposit(user.getId(), amount);
        return ApiResponse.success(SuccessStatus.DEPOSIT_SUCCESS, null);
    }

    @Operation(summary = "지갑 출금", description = "특정 유저의 지갑에서 금액을 출금합니다.")
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal UserDetails userDetails,
                                                      @RequestParam Long amount) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        userWalletService.withdraw(user.getId(), amount);
        return ApiResponse.success(SuccessStatus.WITHDRAW_SUCCESS, null);
    }
}
