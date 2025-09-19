package com.trever.backend.api.trade.service;

import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.Bid;
import com.trever.backend.api.auction.repository.AuctionRepository;
import com.trever.backend.api.auction.repository.BidRepository;
import com.trever.backend.api.trade.dto.PurchaseApplicationRequestDTO;
import com.trever.backend.api.trade.dto.PurchaseApplicationResponseDTO;
import com.trever.backend.api.trade.dto.TransactionResponseDTO;
import com.trever.backend.api.trade.entity.PurchaseApplication;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.PurchaseRequestRepository;
import com.trever.backend.api.trade.repository.TransactionRepository;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.user.service.UserService;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.api.vehicle.service.VehicleService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.trever.backend.api.trade.entity.TransactionStatus.IN_PROGRESS;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ContractService contractService;
    private final UserRepository userRepository;

    // 구매 신청 (일반 거래)
    @Transactional
    public PurchaseApplicationResponseDTO apply(Long vehicleId, Long buyerId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        PurchaseApplication request = PurchaseApplication.builder()
                .buyer(buyer)
                .vehicle(vehicle)
                .build();

        purchaseRequestRepository.save(request);
        return PurchaseApplicationResponseDTO.from(request);
    }

    // 특정 차량의 구매 신청자 목록 조회 (판매자가 보는 화면)
    @Transactional
    public List<PurchaseApplicationResponseDTO> getRequestsByVehicle(Long vehicleId) {
        List<PurchaseApplication> requests = purchaseRequestRepository.findByVehicleId(vehicleId);

        if (requests.isEmpty()) {
            throw new NotFoundException(ErrorStatus.PURCHASE_REQUEST_NOT_FOUND.getMessage());
        }

        return requests.stream()
                .map(PurchaseApplicationResponseDTO::from) // DTO 변환
                .toList();
    }

    // 판매자가 구매자 선택 → 거래 생성
    @Transactional
    public TransactionResponseDTO selectBuyer(Long vehicleId, Long sellerId, Long buyerId) {
        // 차량 조회
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

        // 판매자 검증
        if (!vehicle.getSeller().getId().equals(sellerId)) {
            throw new BadRequestException(ErrorStatus.INVALID_SELLER_SELECTION.getMessage());
        }

        // 구매자 조회
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        // 거래 생성
        Transaction transaction = Transaction.builder()
                .vehicle(vehicle)
                .buyer(buyer)
                .seller(vehicle.getSeller())
                .finalPrice(vehicle.getPrice())
                .status(IN_PROGRESS)
                .build();

        vehicleRepository.updateVehicleStatus(vehicle.getId(),VehicleStatus.IN_PROGRESS);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 계약 생성
        contractService.createContract(savedTransaction.getId());
        return TransactionResponseDTO.from(savedTransaction);
    }

    // 경매 거래 생성
    public TransactionResponseDTO createTransactionFromAuction(Long auctionId) {
        // 1. 경매 ID로 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_AUCTION.getMessage()));

        // 2. 해당 경매에서 최고가 입찰 조회
        Bid winningBid = bidRepository.findHighestBidByAuctionId(auctionId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_BID.getMessage()));

        User buyer = winningBid.getBidder();
        User seller = auction.getVehicle().getSeller();

        // 3. 해당 경매의 차량 정보 조회
        Vehicle vehicle = vehicleRepository.findById(auction.getVehicle().getId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_VEHICLE.getMessage()));

        // 4. Transaction 객체 생성
        Transaction transaction = Transaction.builder()
                .vehicle(vehicle)
                .buyer(buyer)
                .seller(seller)
                .finalPrice(winningBid.getBidPrice())
                .status(IN_PROGRESS)
                .build();

        // 5. 거래 저장 후 계약 생성
        Transaction saved = transactionRepository.save(transaction);
        contractService.createContract(saved.getId());

        return TransactionResponseDTO.from(saved);
    }

    // 거래 조회
    @Transactional
    public TransactionResponseDTO getTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.TRANSACTION_NOT_FOUND.getMessage()));

        return TransactionResponseDTO.from(transaction);
    }
}
