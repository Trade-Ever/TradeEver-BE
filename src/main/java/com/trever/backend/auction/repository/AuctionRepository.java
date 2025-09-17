package com.trever.backend.auction.repository;

import com.trever.backend.auction.entity.Auction;
import com.trever.backend.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    
    List<Auction> findByStatus(AuctionStatus status);
    
    // 상태별 페이징 메서드 추가
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);
    
    // 활성 경매만 조회하기 위한 메서드 (시작시간이 지났고, 종료 시간이 아직 지나지 않은)
    Page<Auction> findByStatusAndStartAtBeforeAndEndAtAfter(
            AuctionStatus status, 
            LocalDateTime startAtBefore, 
            LocalDateTime endAtAfter,
            Pageable pageable);
            
    List<Auction> findByEndAtBefore(LocalDateTime endAtBefore);
}
