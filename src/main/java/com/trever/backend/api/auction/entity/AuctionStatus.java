package com.trever.backend.api.auction.entity;

public enum AuctionStatus {
    UPCOMING,  // 아직 시작되지 않음
    ACTIVE,    // 현재 진행 중
    ENDED,     // 종료됨
    PENDING_CLOSE, //종료 처리 중 (종료되었지만 스케줄러에서 아직 처리되지 않음)
    CANCELLED,  // 취소됨
    EXPIRED // 유찰됨

}
