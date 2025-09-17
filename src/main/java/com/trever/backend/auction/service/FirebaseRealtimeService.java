package com.trever.backend.auction.service;

import com.google.firebase.database.*;
import com.google.api.core.ApiFuture;
import com.trever.backend.auction.dto.BidResponse;
import com.trever.backend.auction.entity.Auction;
import com.trever.backend.auction.entity.Bid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseRealtimeService {

    private final FirebaseDatabase firebaseDatabase;
    private static final String AUCTIONS_REF = "auctions";
    private static final String BIDS_REF = "bids";

    public CompletableFuture<Void> updateAuctionData(Auction auction) {
        CompletableFuture<Void> future = new CompletableFuture<>();
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

        ApiFuture<Void> firebaseFuture = auctionRef.updateChildrenAsync(auctionData);

        firebaseFuture.addListener(() -> {
            try {
                firebaseFuture.get();
                log.info("경매 정보가 Firebase에 성공적으로 업데이트되었습니다. 경매 ID: {}", auction.getId());
                future.complete(null);
            } catch (Exception e) {
                log.error("Firebase 경매 정보 업데이트 실패: {}", e.getMessage());
                future.completeExceptionally(e);
            }
        }, Executors.newSingleThreadExecutor());

        return future;
    }

    public CompletableFuture<Void> addBid(Bid bid, String bidderName) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        DatabaseReference bidRef = firebaseDatabase.getReference(BIDS_REF + "/" + bid.getAuction().getId() + "/" + bid.getId());

        Map<String, Object> bidData = new HashMap<>();
        bidData.put("id", bid.getId());
        bidData.put("bidPrice", bid.getBidPrice());
        bidData.put("bidderId", bid.getBidder().getId());
        bidData.put("bidderName", bidderName);
        bidData.put("createdAt", bid.getCreatedAt().toString());

        ApiFuture<Void> bidFuture = bidRef.setValueAsync(bidData);

        bidFuture.addListener(() -> {
            try {
                bidFuture.get();
                log.info("입찰 정보가 Firebase에 성공적으로 추가되었습니다. 입찰 ID: {}", bid.getId());

                // 이어서 경매 현재가 업데이트
                DatabaseReference auctionRef = firebaseDatabase.getReference(AUCTIONS_REF + "/" + bid.getAuction().getId());

                Map<String, Object> updateData = new HashMap<>();
                updateData.put("currentBidPrice", bid.getBidPrice());
                updateData.put("currentBidUserId", bid.getBidder().getId());
                updateData.put("currentBidUserName", bidderName);
                updateData.put("lastBidTime", bid.getCreatedAt().toString());

                ApiFuture<Void> updateFuture = auctionRef.updateChildrenAsync(updateData);
                updateFuture.addListener(() -> {
                    try {
                        updateFuture.get();
                        log.info("경매 현재가가 Firebase에 성공적으로 업데이트되었습니다. 경매 ID: {}", bid.getAuction().getId());
                        future.complete(null);
                    } catch (Exception e) {
                        log.error("Firebase 경매 현재가 업데이트 실패: {}", e.getMessage());
                        future.completeExceptionally(e);
                    }
                }, Executors.newSingleThreadExecutor());

            } catch (Exception e) {
                log.error("Firebase 입찰 정보 추가 실패: {}", e.getMessage());
                future.completeExceptionally(e);
            }
        }, Executors.newSingleThreadExecutor());

        return future;
    }

    public CompletableFuture<Void> updateAuctionStatus(Long auctionId, String status) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference auctionRef = firebaseDatabase.getReference(AUCTIONS_REF + "/" + auctionId + "/status");

        ApiFuture<Void> statusFuture = auctionRef.setValueAsync(status);

        statusFuture.addListener(() -> {
            try {
                statusFuture.get();
                log.info("경매 상태가 Firebase에서 업데이트되었습니다. 경매 ID: {}, 상태: {}", auctionId, status);
                future.complete(null);
            } catch (Exception e) {
                log.error("Firebase 경매 상태 업데이트 실패: {}", e.getMessage());
                future.completeExceptionally(e);
            }
        }, Executors.newSingleThreadExecutor());

        return future;
    }

    public void listenToAuctionChanges(Long auctionId, AuctionDataListener listener) {
        DatabaseReference auctionRef = firebaseDatabase.getReference(AUCTIONS_REF + "/" + auctionId);

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Float currentBidPrice = dataSnapshot.child("currentBidPrice").getValue(Float.class);
                    Long currentBidUserId = dataSnapshot.child("currentBidUserId").getValue(Long.class);
                    String status = dataSnapshot.child("status").getValue(String.class);

                    listener.onAuctionDataChanged(currentBidPrice, currentBidUserId, status);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Firebase 데이터 변경 이벤트 취소: {}", databaseError.getMessage());
            }
        };

        auctionRef.addValueEventListener(valueEventListener);
    }

    public interface AuctionDataListener {
        void onAuctionDataChanged(Float currentBidPrice, Long currentBidUserId, String status);
    }

    public CompletableFuture<Void> deleteAuctionBids(Long auctionId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference bidsRef = firebaseDatabase.getReference(BIDS_REF + "/" + auctionId);

        ApiFuture<Void> deleteFuture = bidsRef.removeValueAsync();

        deleteFuture.addListener(() -> {
            try {
                deleteFuture.get();
                log.info("경매 입찰 정보가 Firebase에서 삭제되었습니다. 경매 ID: {}", auctionId);
                future.complete(null);
            } catch (Exception e) {
                log.error("Firebase 경매 입찰 정보 삭제 실패: {}", e.getMessage());
                future.completeExceptionally(e);
            }
        }, Executors.newSingleThreadExecutor());

        return future;
    }
}
