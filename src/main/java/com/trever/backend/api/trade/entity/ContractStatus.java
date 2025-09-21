package com.trever.backend.api.trade.entity;

public enum ContractStatus {
    PENDING,             // 대기중 (아무도 서명 X)
    WAITING_FOR_SELLER,  // 구매자 서명 완료
    WAITING_FOR_BUYER,   // 판매자 서명 완료
    COMPLETED,           // 양쪽 다 서명 완료
    CANCELLED            // 계약 취소
}
