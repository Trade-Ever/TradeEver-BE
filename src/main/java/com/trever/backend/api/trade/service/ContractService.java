package com.trever.backend.api.trade.service;

import com.trever.backend.api.trade.dto.ContractResponseDTO;
import com.trever.backend.api.trade.entity.Contract;
import com.trever.backend.api.trade.entity.ContractStatus;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.ContractRepository;
import com.trever.backend.api.trade.repository.TransactionRepository;
import com.trever.backend.api.user.entity.UserProfile;
import com.trever.backend.api.user.repository.UserProfileRepository;
import com.trever.backend.api.user.service.UserWalletService;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.InternalServerException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.util.PdfGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static com.trever.backend.api.trade.entity.TransactionStatus.COMPLETED;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final TransactionRepository transactionRepository;
    private final SpringTemplateEngine templateEngine;
    private final UserProfileRepository userProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final UserWalletService userWalletService;

    // 계약 생성 (거래 확정 시 자동 생성)
    @Transactional
    public ContractResponseDTO createContract(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_TRANSACTION_EXCEPTION.getMessage()));

        // 이미 계약이 존재하면 그대로 반환
        if (transaction.getContract() != null) {
            return ContractResponseDTO.from(transaction.getContract());
        }

        // 신규 게약 생성
        Contract contract = Contract.builder()
                .transaction(transaction)
                .status(ContractStatus.PENDING)
                .signedAt(null)
                .contractPdfUrl(null)
                .build();

        transaction.setContract(contract);
        contractRepository.save(contract);

        return ContractResponseDTO.from(contract);
    }

    // 계약 조회
    @Transactional
    public ContractResponseDTO getContract(Long contractId, Long loginUserId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_CONTRACT_EXCEPTION.getMessage()));

        Transaction transaction = contract.getTransaction();
        if (!transaction.getBuyer().getId().equals(loginUserId) &&
                !transaction.getSeller().getId().equals(loginUserId)) {
            throw new BadRequestException(ErrorStatus.TRANSACTION_ACCESS_DENIED.getMessage());
        }

        return ContractResponseDTO.from(contract);
    }

    // 구매자 서명
    @Transactional
    public ContractResponseDTO signAsBuyer(Long contractId, Long userId) {
        Contract contract = getValidContract(contractId, userId, true);
        contract.signAsBuyer(); // 엔티티 메서드 사용
        checkIfFullySigned(contract);
        return ContractResponseDTO.from(contract);
    }

    // 판매자 서명
    @Transactional
    public ContractResponseDTO signAsSeller(Long contractId, Long userId) {
        Contract contract = getValidContract(contractId, userId, false);
        contract.signAsSeller();
        checkIfFullySigned(contract);
        return ContractResponseDTO.from(contract);
    }

    // 양쪽 다 서명했을 때 최종 처리
    private void checkIfFullySigned(Contract contract) {
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            Transaction transaction = contract.getTransaction();
            Vehicle vehicle = transaction.getVehicle();

            if (vehicle == null) {
                throw new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage());
            }

            Long price = transaction.getFinalPrice();
            Long buyerId = transaction.getBuyer().getId();
            Long sellerId = transaction.getSeller().getId();

            // 돈 이동
            userWalletService.transfer(buyerId, sellerId, price);

            // 구매자, 판매자 프로필 정보 조회
            UserProfile buyerProfile = userProfileRepository.findByUser(transaction.getBuyer())
                    .orElse(null);
            UserProfile sellerProfile = userProfileRepository.findByUser(transaction.getSeller())
                    .orElse(null);

            // 계약서 HTML 생성
            Context context = new Context();
            context.setVariable("transaction", transaction);
            context.setVariable("vehicle", vehicle);
            context.setVariable("contract", contract);
            context.setVariable("buyerProfile", buyerProfile);
            context.setVariable("sellerProfile", sellerProfile);

            String htmlContent = templateEngine.process("contract", context);
            contract.setContractData(htmlContent);

            // PDF 생성
            String uploadDir = "uploads/contracts/";
            String fileName = "contract_" + contract.getId() + ".pdf";
            String filePath = uploadDir + fileName;

            try {
                Files.createDirectories(Paths.get(uploadDir));

                // 기존 파일 삭제 (있으면 지움 → 덮어쓰기)
                Files.deleteIfExists(Paths.get(filePath));
                PdfGenerator.generatePdfFromHtml(htmlContent, filePath);

                // URL 저장
                // API 경로 저장 (앱에서 바로 호출 가능)
                contract.setContractPdfUrl("/api/v1/contracts/" + contract.getId() + "/pdf");
                contract.setSignedAt(LocalDateTime.now());
            } catch (Exception e) {
                throw new InternalServerException(ErrorStatus.PASSPORT_SIGN_ERROR_EXCEPTION.getMessage());
            }

            // Transaction 상태 업데이트
            vehicleRepository.updateVehicleStatus(vehicle.getId(),VehicleStatus.ENDED);
            transaction.setStatus(COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            contractRepository.save(contract);
        }
    }

    private Contract getValidContract(Long contractId, Long userId, boolean isBuyer) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_CONTRACT_EXCEPTION.getMessage()));

        Transaction tx = contract.getTransaction();
        Long expectedId = isBuyer ? tx.getBuyer().getId() : tx.getSeller().getId();

        if (!expectedId.equals(userId)) {
            throw new BadRequestException(isBuyer
                    ? ErrorStatus.INVALID_BUYER_SIGNATURE.getMessage()
                    : ErrorStatus.INVALID_SELLER_SIGNATURE.getMessage());
        }

        return contract;
    }
}
