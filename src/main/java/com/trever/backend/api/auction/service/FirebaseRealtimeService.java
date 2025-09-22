package com.trever.backend.api.auction.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.Bid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseRealtimeService {

    private final FirebaseDatabase firebaseDatabase;

    private static final String AUCTIONS_REF = "auctions";
    private static final String BIDS_REF = "bids";

    /**
     * 경매 데이터 업데이트
     */
    public void updateAuctionData(Auction auction) {
        try {
            DatabaseReference auctionRef = firebaseDatabase.getReference(AUCTIONS_REF + "/" + auction.getId());

            Map<String, Object> auctionData = new HashMap<>();
            auctionData.put("id", auction.getId());
            auctionData.put("startPrice", auction.getStartPrice());
            auctionData.put("startAt", auction.getStartAt().toString());
            auctionData.put("endAt", auction.getEndAt().toString());
            auctionData.put("status", auction.getStatus().name());
            auctionData.put("vehicleId", auction.getVehicle().getId());

            if (auction.getCurrentBidPrice() != null) {
                auctionData.put("currentBidPrice", auction.getCurrentBidPrice());
                auctionData.put("currentBidUserId", auction.getCurrentBidUserId());
            }

            auctionRef.setValueAsync(auctionData);
            log.info("Firebase 경매 데이터 업데이트 성공: {}", auction.getId());
        } catch (Exception e) {
            log.error("Firebase 경매 데이터 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 경매 상태 업데이트
     */
    public void updateAuctionStatus(Long auctionId, String status) {
        try {
            DatabaseReference auctionRef = firebaseDatabase.getReference(AUCTIONS_REF + "/" + auctionId + "/status");
            auctionRef.setValueAsync(status);
            log.info("Firebase 경매 상태 업데이트 - 경매 ID: {}, 상태: {}", auctionId, status);
        } catch (Exception e) {
            log.error("Firebase 경매 상태 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 입찰 추가
     */
    public void addBid(Bid bid, String bidderName) {
        try {
            DatabaseReference bidRef = firebaseDatabase.getReference(BIDS_REF + "/" + bid.getAuction().getId() + "/" + bid.getId());

            Map<String, Object> bidData = new HashMap<>();
            bidData.put("id", bid.getId());
            bidData.put("bidPrice", bid.getBidPrice());
            bidData.put("bidderId", bid.getBidder().getId());
            bidData.put("bidderName", bidderName);
            bidData.put("createdAt", LocalDateTime.now().toString());

            bidRef.setValueAsync(bidData);

            // 경매 테이블의 현재 가격 정보도 업데이트
            DatabaseReference auctionRef = firebaseDatabase.getReference(AUCTIONS_REF + "/" + bid.getAuction().getId());
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("currentBidPrice", bid.getBidPrice());
            updateData.put("currentBidUserId", bid.getBidder().getId());
            updateData.put("currentBidUserName", bidderName);
            updateData.put("lastBidTime", LocalDateTime.now().toString());

            auctionRef.updateChildrenAsync(updateData);

            log.info("Firebase 입찰 추가 및 현재가 업데이트 - 경매 ID: {}, 입찰 ID: {}, 입찰자: {}, 입찰가: {}",
                    bid.getAuction().getId(), bid.getId(), bidderName, bid.getBidPrice());
        } catch (Exception e) {
            log.error("Firebase 입찰 추가 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 현재 가격 업데이트
     */
    public void updateCurrentPrice(Long auctionId, Long price) {
        try {
            DatabaseReference auctionRef = firebaseDatabase.getReference(AUCTIONS_REF + "/" + auctionId);
            auctionRef.child("currentBidPrice").setValueAsync(price);
            log.info("Firebase 현재 가격 업데이트 - 경매 ID: {}, 가격: {}", auctionId, price);
        } catch (Exception e) {
            log.error("Firebase 현재 가격 업데이트 실패: {}", e.getMessage(), e);
        }
    }
}
