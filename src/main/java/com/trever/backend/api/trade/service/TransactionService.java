package com.trever.backend.api.trade.service;

import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.Bid;
import com.trever.backend.api.auction.repository.AuctionRepository;
import com.trever.backend.api.auction.repository.BidRepository;
import com.trever.backend.api.trade.dto.PurchaseApplicationRequestDTO;
import com.trever.backend.api.trade.dto.PurchaseApplicationResponseDTO;
import com.trever.backend.api.trade.entity.PurchaseApplication;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.PurchaseRequestRepository;
import com.trever.backend.api.trade.repository.TransactionRepository;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ContractService contractService;

    // 구매 신청 (일반 거래)
    public PurchaseApplication apply(PurchaseApplicationRequestDTO purchaseApplicationRequestDTO) {
        PurchaseApplication request = PurchaseApplication.builder()
                .buyerId(purchaseApplicationRequestDTO.getBuyerId())
                .vehicleId(purchaseApplicationRequestDTO.getVehicleId())
                .build();

        return purchaseRequestRepository.save(request);
    }

    // 특정 차량의 구매 신청자 목록 조회
    public List<PurchaseApplicationResponseDTO> getRequestsByVehicle(Long vehicleId) {
        List<PurchaseApplication> requests = purchaseRequestRepository.findByVehicleId(vehicleId);

        return requests.stream()
                .map(PurchaseApplicationResponseDTO::from) // DTO 변환
                .toList();
    }

    // 판매자가 구매자 선택 → 거래 생성
    public Transaction selectBuyer(Long requestId) {
        PurchaseApplication request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PURCHASE_REQUEST_NOT_FOUND.getMessage()));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

        Transaction tx = Transaction.builder()
                .buyerId(request.getBuyerId())
                .sellerId(vehicle.getSellerId())
                .vehicleId(vehicle.getId())
                .finalPrice(vehicle.getPrice())
                .status("PENDING")
                .build();

        Transaction saved = transactionRepository.save(tx);
        contractService.createContract(saved);
        return saved;
    }

    // 경매 거래 생성
    public Transaction createTransactionFromAuction(Long auctionId) {
        // 1. 경매 ID로 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_AUCTION.getMessage()));

        // 2. 해당 경매에서 최고가 입찰 조회
        Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidPriceDesc(auctionId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_BID.getMessage()));

        // 3. 해당 경매의 차량 정보 조회
        Vehicle vehicle = vehicleRepository.findById(auction.getVehicleId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_VEHICLE.getMessage()));

        // 4. Transaction 객체 생성
        Transaction tx = new Transaction();
        tx.setBuyerId(highestBid.getBidderId()); // 최고가 입찰자
        tx.setSellerId(vehicle.getSellerId());   // 차량 등록자(판매자)
        tx.setVehicleId(vehicle.getId());
        tx.setFinalPrice(highestBid.getBidPrice()); // 최종 거래가 = 낙찰가
        tx.setStatus("PENDING");

        // 5. 거래 저장 후 계약 생성
        Transaction saved = transactionRepository.save(tx);
        contractService.createContract(saved);
        return saved;
    }

    // 거래 조회
    public Transaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.TRANSACTION_NOT_FOUND.getMessage()));

    }
}
