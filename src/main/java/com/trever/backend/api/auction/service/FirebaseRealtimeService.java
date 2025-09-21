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
    
    /**
     * 경매 데이터 업데이트
     */
    public void updateAuctionData(Auction auction) {
        try {
            DatabaseReference auctionsRef = firebaseDatabase.getReference("auctions");
            DatabaseReference auctionRef = auctionsRef.child(auction.getId().toString());
            
            Map<String, Object> auctionData = new HashMap<>();
            auctionData.put("id", auction.getId());
            auctionData.put("startPrice", auction.getStartPrice());
            auctionData.put("currentBidPrice", auction.getCurrentBidPrice());
            auctionData.put("startAt", auction.getStartAt().toEpochSecond(ZoneOffset.UTC));
            auctionData.put("endAt", auction.getEndAt().toEpochSecond(ZoneOffset.UTC));
            auctionData.put("status", auction.getStatus().name());
            auctionData.put("vehicleId", auction.getVehicle().getId());
            
            auctionRef.setValueAsync(auctionData);
        } catch (Exception e) {
            log.error("Firebase 경매 데이터 업데이트 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 경매 상태 업데이트
     */
    public void updateAuctionStatus(Long auctionId, String status) {
        try {
            DatabaseReference auctionRef = firebaseDatabase.getReference("auctions/" + auctionId);
            auctionRef.child("status").setValueAsync(status);
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
            DatabaseReference bidsRef = firebaseDatabase.getReference("auctions/" + bid.getAuction().getId() + "/bids");
            DatabaseReference bidRef = bidsRef.push();
            
            Map<String, Object> bidData = new HashMap<>();
            bidData.put("id", bid.getId());
            bidData.put("bidPrice", bid.getBidPrice());
            bidData.put("bidderId", bid.getBidder().getId());
            bidData.put("bidderName", bidderName);
            bidData.put("bidTime", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
            
            bidRef.setValueAsync(bidData);
            log.info("Firebase 입찰 추가 - 경매 ID: {}, 입찰 ID: {}, 입찰자: {}, 입찰가: {}", 
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
            DatabaseReference auctionRef = firebaseDatabase.getReference("auctions/" + auctionId);
            auctionRef.child("currentPrice").setValueAsync(price);
            log.info("Firebase 현재 가격 업데이트 - 경매 ID: {}, 가격: {}", auctionId, price);
        } catch (Exception e) {
            log.error("Firebase 현재 가격 업데이트 실패: {}", e.getMessage(), e);
        }
    }
}
