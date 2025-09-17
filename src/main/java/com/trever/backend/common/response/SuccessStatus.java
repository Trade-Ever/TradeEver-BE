package com.trever.backend.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

    /**
     * 200
     */
    SEND_REGISTER_SUCCESS(HttpStatus.OK,"회원가입 성공"),
    SEND_LOGIN_SUCCESS(HttpStatus.OK, "로그인 성공"),
    SEND_REISSUE_TOKEN_SUCCESS(HttpStatus.OK,"토큰 재발급 성공"),
    SEND_HEALTH_SUCCESS(HttpStatus.OK,"서버 상태 OK"),
    AUCTION_CREATED(HttpStatus.OK,"경매 생성"),
    AUCTION_READ(HttpStatus.OK,"경매 조회"),
    BID_SUCESS(HttpStatus.OK,"입찰 성공"),
    AUCTION_CANCEL(HttpStatus.OK,"경매 취소"),
    CAR_INFO_SUCCESS(HttpStatus.OK, "차량 정보 조회 성공"),
    CAR_HIERARCHY_SUCCESS(HttpStatus.OK, "차량 계층 구조 조회 성공"),
    // SuccessStatus 열거형에 차량 관련 상태 추가
    VEHICLE_CREATED(HttpStatus.OK, "차량 등록 성공"),
    VEHICLE_READ(HttpStatus.OK, "차량 조회 성공"),
    VEHICLE_DELETED(HttpStatus.OK, "차량 삭제 성공"),
    SEND_CONTRACT_SUCCESS(HttpStatus.OK,"계약 조회 성공"),
    SIGN_CONTRACT_SUCCESS(HttpStatus.OK,"계약 서명 성공"),
    TRANSACTION_CREATE_SUCCESS(HttpStatus.OK, "거래 생성 성공"),
    TRANSACTION_GET_SUCCESS(HttpStatus.OK, "거래 조회 성공"),
    SEND_CONTRACT_PDF_SUCCESS(HttpStatus.OK, "계약서 PDF 조회 성공"),
    AUCTION_TRANSACTION_CREATE_SUCCESS(HttpStatus.OK, "경매 거래 생성 성공"),
    PURCHASE_REQUEST_CREATE_SUCCESS(HttpStatus.OK, "구매 신청 성공"),
    PURCHASE_REQUEST_LIST_SUCCESS(HttpStatus.OK, "구매 신청자 목록 조회 성공"),
    SEND_MEMBER_SUCCESS(HttpStatus.OK, "사용자 정보 조회 성공"),


    /**
     * 201
     */

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}