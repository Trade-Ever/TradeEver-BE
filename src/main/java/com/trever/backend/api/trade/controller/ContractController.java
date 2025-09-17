package com.trever.backend.api.trade.controller;

import com.trever.backend.api.trade.dto.ContractResponseDTO;
import com.trever.backend.api.trade.entity.Contract;
import com.trever.backend.api.trade.service.ContractService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contract", description = "계약 관련 API입니다.")
public class ContractController {

    private final ContractService contractService;

    // 거래에 연결된 계약 조회
    @Operation(summary = "계약 조회 API", description = "계약을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponseDTO>> getContract(@PathVariable Long id) {
        Contract contract = contractService.getContract(id);
        return ApiResponse.success(
                SuccessStatus.SEND_CONTRACT_SUCCESS,
                ContractResponseDTO.from(contract)
        );
    }

    // 구매자 서명
    @Operation(summary = "구매자 서명 API", description = "계약에 구매자가 서명합니다.")
    @PostMapping("/{id}/sign/buyer")
    public ResponseEntity<ApiResponse<ContractResponseDTO>> signAsBuyer(@PathVariable Long id) {
        Contract contract = contractService.signAsBuyer(id);
        return ApiResponse.success(
                SuccessStatus.SIGN_CONTRACT_SUCCESS,
                ContractResponseDTO.from(contract)
        );
    }

    // 판매자 서명
    @Operation(summary = "판매자 서명 API", description = "계약에 판매자가 서명합니다.")
    @PostMapping("/{id}/sign/seller")
    public ResponseEntity<ApiResponse<ContractResponseDTO>> signAsSeller(@PathVariable Long id) {
        Contract contract = contractService.signAsSeller(id);
        return ApiResponse.success(
                SuccessStatus.SIGN_CONTRACT_SUCCESS,
                ContractResponseDTO.from(contract)
        );
    }

//    // 계약서 pdf url 조희
//    @Operation(summary = "계약서 PDF URL API", description = "계약서를 PDF 파일로 조회합니다.")
//    @GetMapping("/{id}/pdf")
//    public ResponseEntity<ApiResponse<String>> getContractPdfUrl(@PathVariable Long id) {
//        Contract contract = contractService.getContract(id);
//
//        if (contract.getContractPdfUrl() == null) {
//            throw new BadRequestException(ErrorStatus.CONTRACT_PDF_GENERATION_FAILED.getMessage());
//        }
//
//        return ApiResponse.success(SuccessStatus.SEND_CONTRACT_PDF_SUCCESS, contract.getContractPdfUrl());
//    }
}
