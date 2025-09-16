package com.trever.backend.api.trade.service;

import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.Bid;
import com.trever.backend.api.auction.repository.AuctionRepository;
import com.trever.backend.api.auction.repository.BidRepository;
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
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
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
