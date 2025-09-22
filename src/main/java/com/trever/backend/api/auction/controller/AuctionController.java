package com.trever.backend.api.auction.controller;

import com.trever.backend.api.auction.dto.*;
import com.trever.backend.api.auction.entity.AuctionStatus;
import com.trever.backend.api.auction.service.AuctionService;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.response.SuccessStatus;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Tag(name = "Auction", description = "경매 API")
@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    
//    @Operation(summary = "경매 생성", description = "새로운 경매를 생성합니다.")
//    @PostMapping
//    public ResponseEntity<ApiResponse<Long>> createAuction(
//            @Valid @RequestBody AuctionCreateRequest request) {
//
//        // 그리고 Vehicle 정보도 조회해야 함
//         Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow(NotFoundException::new);
//
//        // 테스트를 위해 null을 넘기고 있지만, 실제로는 위에서 조회한 vehicle을 넘겨야 함
//        Long auctionId = auctionService.createAuction(request, vehicle);
//        return ApiResponse.success(SuccessStatus.AUCTION_CREATED, auctionId);
//    }
    
    @Operation(summary = "경매 상세 조회", description = "경매 상세 정보를 조회합니다.")
    @GetMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionDetailResponse>> getAuctionDetail(
            @PathVariable Long auctionId) {
        AuctionDetailResponse auction = auctionService.getAuctionDetail(auctionId);
        return ApiResponse.success(SuccessStatus.AUCTION_READ, auction);
    }
    
    @Operation(summary = "경매 목록 조회", description = "경매 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AuctionListResponse>> getAuctions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        AuctionStatus auctionStatus = null;
        if (status != null) {
            try {
                auctionStatus = AuctionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 잘못된 상태값이면 null로 처리 (전체 조회)
            }
        }
        
        AuctionListResponse auctions = auctionService.getAuctions(auctionStatus, page, size);
        return ApiResponse.success(SuccessStatus.AUCTION_READ, auctions);
    }

    /**
     * 입찰하기 - 비동기 처리 후 결과 반환
     */
    @PostMapping("/bids")
    public ResponseEntity<ApiResponse<?>> placeBid(
            @Valid @RequestBody BidRequest bidRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            Long auctionId = bidRequest.getAuctionId();
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

            bidRequest.setAuctionId(auctionId);
            
            // CompletableFuture 결과를 동기적으로 대기하여 예외를 즉시 포착
            BidResponse response = auctionService.placeBid(bidRequest, user).join();
            
            return ResponseEntity.ok(ApiResponse.<BidResponse>builder()
                    .status(SuccessStatus.CREATE_BID_SUCCESS.getStatusCode())
                    .message(SuccessStatus.CREATE_BID_SUCCESS.getMessage())
                    .data(response)
                    .build());
                    
        } catch (CompletionException ex) {
            // CompletableFuture에서 발생한 예외 처리
            Throwable cause = ex.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            
            log.error("입찰 처리 중 오류 발생: {}", cause.getMessage());
            
            // 모든 예외를 400 Bad Request로 응답
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .status(400)
                            .message(cause.getMessage())
                            .build());
                            
        } catch (Exception e) {
            // 기타 예외 처리
            log.error("입찰 요청 처리 중 오류 발생: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }
    
    @Operation(summary = "경매 취소", description = "경매를 취소합니다. 관리자 또는 판매자만 가능합니다.")
    @PostMapping("/{auctionId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAuction(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal UserDetails userDetails

    ) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        // 실제 구현 시에는 인증된 사용자의 권한 체크 필요
        auctionService.cancelAuction(auctionId,user.getId());
        return ApiResponse.success_only(SuccessStatus.AUCTION_CANCEL);
    }
}
