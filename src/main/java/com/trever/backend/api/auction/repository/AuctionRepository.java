package com.trever.backend.api.auction.repository;

import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // 특정 상태이고 시작 시간이 특정 시간 이전인 경매 조회
    List<Auction> findByStatusAndStartAtBefore(AuctionStatus status, LocalDateTime startAtBefore);

    // 특정 상태이고 시작 시간이 특정 시간과 정확히 일치하는 경매 조회
    List<Auction> findByStatusAndStartAtEquals(AuctionStatus status, LocalDateTime startAt);

    // 특정 상태이고 종료 시간이 특정 시간 이전인 경매 조회
    List<Auction> findByStatusAndEndAtBefore(AuctionStatus status, LocalDateTime endAtBefore);

    // 종료 시간이 특정 범위 내에 있는 경매 조회
    List<Auction> findByStatusAndEndAtBetween(AuctionStatus status, LocalDateTime from, LocalDateTime to);

}
