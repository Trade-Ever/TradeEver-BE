package com.trever.backend.api.trade.service;

import com.trever.backend.api.trade.dto.ContractResponseDTO;
import com.trever.backend.api.trade.entity.Contract;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.ContractRepository;
import com.trever.backend.api.trade.repository.TransactionRepository;
import com.trever.backend.api.user.entity.UserProfile;
import com.trever.backend.api.user.repository.UserProfileRepository;
import com.trever.backend.api.user.service.UserService;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.api.vehicle.service.VehicleService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.InternalServerException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.util.PdfGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
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
                .signedByBuyer(false)
                .signedBySeller(false)
                .signedAt(null)
                .contractPdfUrl(null)
                .build();

        transaction.setContract(contract);
        contractRepository.save(contract);

        return ContractResponseDTO.from(contract);
    }

    // 계약 조회
    @Transactional
    public ContractResponseDTO getContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_CONTRACT_EXCEPTION.getMessage()));

        return ContractResponseDTO.from(contract);
    }

    // 구매자 서명
    @Transactional
    public ContractResponseDTO signAsBuyer(Long contractId, Long userId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_CONTRACT_EXCEPTION.getMessage()));

        Transaction transaction = contract.getTransaction();
        if (!transaction.getBuyer().getId().equals(userId)) {
            throw new BadRequestException(ErrorStatus.INVALID_BUYER_SIGNATURE.getMessage());
        }

        contract.signAsBuyer(); // 엔티티 메서드 사용
        checkIfFullySigned(contract);
        return ContractResponseDTO.from(contract);
    }

    // 판매자 서명
    @Transactional
    public ContractResponseDTO signAsSeller(Long contractId, Long userId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_CONTRACT_EXCEPTION.getMessage()));

        Transaction transaction = contract.getTransaction();
        if (!transaction.getSeller().getId().equals(userId)) {
            throw new BadRequestException(ErrorStatus.INVALID_SELLER_SIGNATURE.getMessage());
        }

        contract.signAsSeller();
        checkIfFullySigned(contract);
        return ContractResponseDTO.from(contract);
    }

    // 양쪽 다 서명했을 때 최종 처리
    private void checkIfFullySigned(Contract contract) {
        if (contract.isSignedByBuyer() && contract.isSignedBySeller()) {
            Transaction transaction = contract.getTransaction();

            Vehicle vehicle = transaction.getVehicle();
            if (vehicle == null) {
                throw new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage());
            }

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
                contract.setContractPdfUrl("uploads/contracts/" + fileName);
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
}
