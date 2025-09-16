package com.trever.backend.api.auction.repository;

import com.trever.backend.api.auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
