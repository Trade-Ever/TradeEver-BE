package com.trever.backend.api.auction.repository;

import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    
    List<Bid> findByAuctionIdOrderByBidPriceDesc(Long auctionId);
    
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.bidPrice DESC, b.createdAt ASC")
    List<Bid> findTopBidsByAuctionId(Long auctionId);
    
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.bidPrice DESC, b.createdAt ASC LIMIT 1")
    Optional<Bid> findHighestBidByAuctionId(Long auctionId);

    // 경매별 입찰 목록 조회
    List<Bid> findByAuctionOrderByBidPriceDesc(Auction auction);

    // 경매별 입찰 수 조회
    int countByAuction(Auction auction);

}
