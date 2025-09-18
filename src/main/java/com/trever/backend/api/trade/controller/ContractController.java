package com.trever.backend.api.trade.controller;

import com.trever.backend.api.trade.dto.ContractResponseDTO;
import com.trever.backend.api.trade.entity.Contract;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.ContractRepository;
import com.trever.backend.api.trade.service.ContractService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contract", description = "계약 관련 API입니다.")
public class ContractController {

    private final ContractService contractService;
    private final ContractRepository contractRepository;

    // 거래에 연결된 계약 조회
    @Operation(summary = "계약 조회 API", description = "계약을 조회합니다.")
    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponseDTO>> getContract(@PathVariable Long contractId) {

        ContractResponseDTO contractResponseDTO = contractService.getContract(contractId);
        return ApiResponse.success(SuccessStatus.SEND_CONTRACT_SUCCESS, contractResponseDTO);
    }

    // 구매자 서명
    @Operation(summary = "구매자 서명 API", description = "계약에 구매자가 서명합니다.")
    @PostMapping("/{contractId}/sign/buyer")
    public ResponseEntity<ApiResponse<ContractResponseDTO>> signAsBuyer(
            @PathVariable Long contractId,
            @RequestParam Long userId) {

        ContractResponseDTO contractResponseDTO = contractService.signAsBuyer(contractId, userId);
        return ApiResponse.success(SuccessStatus.SIGN_CONTRACT_SUCCESS, contractResponseDTO);
    }

    // 판매자 서명
    @Operation(summary = "판매자 서명 API", description = "계약에 판매자가 서명합니다.")
    @PostMapping("/{contractId}/sign/seller")
    public ResponseEntity<ApiResponse<ContractResponseDTO>> signAsSeller(
            @PathVariable Long contractId,
            @RequestParam Long userId) {
        ContractResponseDTO contractResponseDTO = contractService.signAsSeller(contractId, userId);
        return ApiResponse.success(SuccessStatus.SIGN_CONTRACT_SUCCESS, contractResponseDTO);
    }

    // 계약서 pdf 조희
    @Operation(summary = "계약서 PDF 조회 API", description = "구매자 또는 판매자가 계약 체결 후 생성된 계약서 PDF를 조회합니다")
    @GetMapping("/{contractId}/pdf")
    public ResponseEntity<Resource> getContractPdf(
            @PathVariable Long contractId,
            @RequestParam Long userId) throws MalformedURLException {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_CONTRACT_EXCEPTION.getMessage()));

        if (contract.getContractPdfUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        Transaction tx = contract.getTransaction();
        if (!tx.getBuyer().getId().equals(userId) && !tx.getSeller().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 파일 읽어서 반환
        String fileName = Paths.get(contract.getContractPdfUrl()).getFileName().toString();
        Path filePath = Paths.get("uploads/contracts").resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName) // 브라우저에서 바로 열기
                .body(resource);
    }

}
