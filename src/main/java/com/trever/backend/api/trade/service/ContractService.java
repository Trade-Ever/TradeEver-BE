package com.trever.backend.api.trade.service;

import com.trever.backend.api.trade.dto.ContractResponseDTO;
import com.trever.backend.api.trade.entity.Contract;
import com.trever.backend.api.trade.entity.ContractStatus;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.entity.TransactionStatus;
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
                .status(ContractStatus.COMPLETED)
                .signedAt(LocalDateTime.now())
//                .contractPdfUrl(null)
                .build();

        transaction.setContract(contract);
        contractRepository.save(contract);

        // PDF 생성 로직 바로 실행
        generatePdf(contract, transaction);

        // 거래 상태도 완료 처리
        Vehicle vehicle = transaction.getVehicle();
        vehicleRepository.updateVehicleStatus(vehicle.getId(), VehicleStatus.ENDED);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        return ContractResponseDTO.from(contract);
    }

    private void generatePdf(Contract contract, Transaction transaction) {
        Vehicle vehicle = transaction.getVehicle();

        // 프로필 등 필요한 데이터 조회
        UserProfile buyerProfile = userProfileRepository.findByUser(transaction.getBuyer()).orElse(null);
        UserProfile sellerProfile = userProfileRepository.findByUser(transaction.getSeller()).orElse(null);

        Context context = new Context();
        context.setVariable("transaction", transaction);
        context.setVariable("vehicle", vehicle);
        context.setVariable("contract", contract);
        context.setVariable("buyerProfile", buyerProfile);
        context.setVariable("sellerProfile", sellerProfile);

        String htmlContent = templateEngine.process("contract", context);
        contract.setContractData(htmlContent);

        try {
            String uploadDir = "uploads/contracts/";
            Files.createDirectories(Paths.get(uploadDir));

            String fileName = "contract_" + contract.getId() + ".pdf";
            String filePath = uploadDir + fileName;

            PdfGenerator.generatePdfFromHtml(htmlContent, filePath);

            contract.setContractPdfUrl("/api/v1/contracts/" + contract.getId() + "/pdf");
            contractRepository.save(contract);
        } catch (Exception e) {
            throw new InternalServerException("PDF 생성 실패");
        }
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

//    // 구매자 서명
//    @Transactional
//    public ContractResponseDTO signAsBuyer(Long contractId, Long userId) {
//        Contract contract = getValidContract(contractId, userId, true);
//        contract.signAsBuyer(); // 엔티티 메서드 사용
//        checkIfFullySigned(contract);
//        return ContractResponseDTO.from(contract);
//    }
//
//    // 판매자 서명
//    @Transactional
//    public ContractResponseDTO signAsSeller(Long contractId, Long userId) {
//        Contract contract = getValidContract(contractId, userId, false);
//        contract.signAsSeller();
//        checkIfFullySigned(contract);
//        return ContractResponseDTO.from(contract);
//    }
}
