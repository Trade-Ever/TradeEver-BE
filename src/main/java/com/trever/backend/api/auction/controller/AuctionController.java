package com.trever.backend.api.auction.controller;

import com.trever.backend.api.auction.dto.*;
import com.trever.backend.api.auction.dto.*;
import com.trever.backend.api.auction.entity.AuctionStatus;
import com.trever.backend.api.auction.service.AuctionService;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auction", description = "경매 API")
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
    
    @Operation(summary = "입찰하기", description = "경매에 입찰을 등록합니다.")
    @PostMapping("/bid")
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(
            @Valid @RequestBody BidRequest request) {
        // 실제 구현 시에는 인증된 사용자의 정보를 사용해야 함
         User bidder = userRepository.findById(request.getBidderId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
            
        // 테스트를 위해 null을 넘기고 있지만, 실제로는 위에서 조회한 bidder를 넘겨야 함
        BidResponse bidResponse = auctionService.placeBid(request, bidder);
        return ApiResponse.success(SuccessStatus.BID_SUCESS, bidResponse);
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
