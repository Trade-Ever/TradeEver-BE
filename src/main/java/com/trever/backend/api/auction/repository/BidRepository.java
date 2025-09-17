package com.trever.backend.api.auction.repository;

import com.trever.backend.api.auction.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    Optional<Bid> findTopByAuctionIdOrderByBidPriceDesc(Long auctionId);
}
