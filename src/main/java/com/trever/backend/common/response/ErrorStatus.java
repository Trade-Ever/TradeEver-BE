package com.trever.backend.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)

public enum ErrorStatus {

    /**
     * 400 BAD_REQUEST
     */
    VALIDATION_REQUEST_MISSING_EXCEPTION(HttpStatus.BAD_REQUEST, "요청 값이 입력되지 않았습니다."),
    ALREADY_REGISTERED_ACCOUNT_EXCEPTION(HttpStatus.BAD_REQUEST, "이미 회원가입된 이메일입니다."),
    USER_ALREADY_LOGGED_OUT(HttpStatus.BAD_REQUEST, "이미 로그아웃 되었습니다."),
    NOT_MATCHED_LOGIN_USER_EXCEPTION(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호가 일치하지 않습니다."),
    NOT_REGISTER_USER_EXCEPTION(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자 입니다."),
    PASSWORD_MISMATCH_EXCEPTION(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    INVALID_SELLER_SELECTION(HttpStatus.BAD_REQUEST, "판매자만 구매자를 선택할 수 있습니다."),
    INVALID_BUYER_SIGNATURE(HttpStatus.BAD_REQUEST, "구매자만 서명할 수 있습니다."),
    INVALID_SELLER_SIGNATURE(HttpStatus.BAD_REQUEST, "판매자만 서명할 수 있습니다."),
    FAVORITE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 찜한 차량입니다."),
    INSUFFICIENT_FUNDS(HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),
    TRANSACTION_ACCESS_DENIED(HttpStatus.BAD_REQUEST,"해당 거래에 접근 권한이 없습니다."),
    PURCHASE_REQUEST_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,"이미 신청한 거래입니다."),
    CANNOT_APPLY_OWN_VEHICLE(HttpStatus.BAD_REQUEST,"자신이 등록한 차량에는 구매 신청을 할 수 없습니다."),
    BID_CANCLED(HttpStatus.BAD_REQUEST,"입찰실패"),

    /**
     * 401 UNAUTHORIZED
     */
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"인증되지 않은 사용자입니다."),
    UNAUTHORIZED_REFRESH_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED,"유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED,"만료된 토큰입니다."),
    INVALID_SIGNATURE_EXCEPTION(HttpStatus.UNAUTHORIZED,"비정상적인 서명입니다."),
    MALFORMED_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED,"유효하지 않은 토큰입니다."),

    /**
     * 403 FORBIDDEN
     */

    /**
     * 404 NOT_FOUND
     */
    NOT_LOGIN_EXCEPTION(HttpStatus.NOT_FOUND,"로그인이 필요합니다."),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다."),
    NOT_FOUND_CONTRACT_EXCEPTION(HttpStatus.NOT_FOUND, "계약을 찾을 수 없습니다."),
    NOT_FOUND_TRANSACTION_EXCEPTION(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다."),
    NOT_FOUND_AUCTION(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."),
    NOT_FOUND_BID(HttpStatus.NOT_FOUND, "입찰 내역이 없습니다."),
    NOT_FOUND_VEHICLE(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다."),
    PURCHASE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "구매 신청을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자 입니다."),
    USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "찜을 찾을 수 없습니다."),

    /**
     * 500 SERVER_ERROR
     */
    PASSPORT_SIGN_ERROR_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"Passport 서명 검증 중 오류가 발생했습니다."),
    CONTRACT_PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "계약서 PDF 생성 중 오류가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}