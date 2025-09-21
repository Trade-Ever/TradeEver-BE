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
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Auction", description = "경매 API")
@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    
    @Operation(summary = "경매 생성", description = "새로운 경매를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createAuction(
            @Valid @RequestBody AuctionCreateRequest request) {
        // 실제 구현 시에는 인증된 사용자의 정보와 권한 체크를 해야 함
        // 그리고 Vehicle 정보도 조회해야 함
         Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow(NotFoundException::new);

        // 테스트를 위해 null을 넘기고 있지만, 실제로는 위에서 조회한 vehicle을 넘겨야 함
        Long auctionId = auctionService.createAuction(request, vehicle);
        return ApiResponse.success(SuccessStatus.AUCTION_CREATED, auctionId);
    }
    
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
    public DeferredResult<ResponseEntity<?>> placeBid(
            @Valid @RequestBody BidRequest bidRequest
//            @AuthenticationPrincipal User user
    ) {

        Long auctionId = bidRequest.getAuctionId();
        User user =  userRepository.findById(1L).orElseThrow(NotFoundException::new);

        // 최대 10초까지 결과를 기다리는 DeferredResult 생성
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(10000L);

        // 타임아웃 설정
        deferredResult.onTimeout(() -> {
            deferredResult.setResult(ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("입찰 처리 중입니다. 상태를 확인해주세요."));
        });

        bidRequest.setAuctionId(auctionId);

        try {
            // 비동기 입찰 큐에 요청 등록
            CompletableFuture<BidResponse> responseFuture =
                    auctionService.placeBid(bidRequest, user);

            // 결과가 완료되면 DeferredResult에 설정
            responseFuture.whenComplete((result, ex) -> {
                if (ex != null) {
                    // 예외 발생 시
                    log.error("입찰 처리 중 오류 발생: {}", ex.getMessage(), ex);
                    deferredResult.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ex.getMessage()));
                } else {
                    // 정상 처리 시
                    log.debug("입찰 처리 완료: 경매 ID={}, 입찰자={}, 입찰가={}",
                            auctionId, user.getName(), result.getBidPrice());
                    deferredResult.setResult(ResponseEntity.ok(result));
                }
            });

        } catch (Exception e) {
            // 입찰 요청 등록 자체에 실패한 경우
            log.error("입찰 요청 처리 중 오류 발생: {}", e.getMessage(), e);
            deferredResult.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage()));
        }

        return deferredResult;
    }
    
    @Operation(summary = "경매 취소", description = "경매를 취소합니다. 관리자 또는 판매자만 가능합니다.")
    @PostMapping("/{auctionId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAuction(
            @PathVariable Long auctionId) {
        // 실제 구현 시에는 인증된 사용자의 권한 체크 필요
        auctionService.cancelAuction(auctionId);
        return ApiResponse.success_only(SuccessStatus.AUCTION_CANCEL);
    }
}
