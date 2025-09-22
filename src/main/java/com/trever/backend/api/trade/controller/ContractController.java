package com.trever.backend.api.trade.controller;

import com.trever.backend.api.trade.dto.ContractResponseDTO;
import com.trever.backend.api.trade.entity.Contract;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.ContractRepository;
import com.trever.backend.api.trade.service.ContractService;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.user.service.UserService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository;

    // 거래에 연결된 계약 조회
    @Operation(summary = "계약 조회 API", description = "계약을 조회합니다.")
    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponseDTO>> getContract(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User loginUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        ContractResponseDTO contractResponseDTO = contractService.getContract(contractId, loginUser.getId());
        return ApiResponse.success(SuccessStatus.SEND_CONTRACT_SUCCESS, contractResponseDTO);
    }

//    // 구매자 서명
//    @Operation(summary = "구매자 서명 API", description = "계약에 구매자가 서명합니다.")
//    @PostMapping("/{contractId}/sign/buyer")
//    public ResponseEntity<ApiResponse<ContractResponseDTO>> signAsBuyer(
//            @PathVariable Long contractId,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        String email = userDetails.getUsername();
//        User buyer = userRepository.findByEmail(email)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
//
//        ContractResponseDTO contractResponseDTO = contractService.signAsBuyer(contractId, buyer.getId());
//        return ApiResponse.success(SuccessStatus.SIGN_CONTRACT_SUCCESS, contractResponseDTO);
//    }
//
//    // 판매자 서명
//    @Operation(summary = "판매자 서명 API", description = "계약에 판매자가 서명합니다.")
//    @PostMapping("/{contractId}/sign/seller")
//    public ResponseEntity<ApiResponse<ContractResponseDTO>> signAsSeller(
//            @PathVariable Long contractId,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        String email = userDetails.getUsername();
//        User seller = userRepository.findByEmail(email)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
//
//        ContractResponseDTO contractResponseDTO = contractService.signAsSeller(contractId, seller.getId());
//        return ApiResponse.success(SuccessStatus.SIGN_CONTRACT_SUCCESS, contractResponseDTO);
//    }

    // 계약서 pdf 조희
    @Operation(summary = "계약서 PDF 조회 API", description = "구매자 또는 판매자가 계약 체결 후 생성된 계약서 PDF를 조회합니다")
    @GetMapping("/{contractId}/pdf")
    public ResponseEntity<Resource> getContractPdf(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails userDetails) throws MalformedURLException {

        String email = userDetails.getUsername();
        User loginUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_CONTRACT_EXCEPTION.getMessage()));

        if (contract.getContractPdfUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        Transaction tx = contract.getTransaction();
        if (!tx.getBuyer().getId().equals(loginUser.getId()) &&
                !tx.getSeller().getId().equals(loginUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 저장된 경로는 상대경로 (/api/v1/contracts/1/pdf), 실제 파일 경로는 uploads/contracts
        String fileName = "contract_" + contract.getId() + ".pdf";
        Path filePath = Paths.get("uploads/contracts").resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName) // 브라우저에서 바로 열기
                .body(resource);
    }

}
