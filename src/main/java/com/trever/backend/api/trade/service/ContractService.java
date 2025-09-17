package com.trever.backend.api.trade.service;

import com.trever.backend.api.trade.entity.Contract;
import com.trever.backend.api.trade.entity.Transaction;
import com.trever.backend.api.trade.repository.ContractRepository;
import com.trever.backend.api.trade.repository.TransactionRepository;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.common.exception.InternalServerException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.util.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final TransactionRepository transactionRepository;
    private final VehicleRepository vehicleRepository;
    private final SpringTemplateEngine templateEngine;

    // 계약 생성
    public Contract createContract(Transaction transaction) {
        Contract contract = new Contract();
        contract.setTransaction(transaction);
        contract.setSignedByBuyer(false);
        contract.setSignedBySeller(false);

        return contractRepository.save(contract);
    }

    // 계약서 조회
    public Contract getContract(Long transactionId) {
        return contractRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.CONTRACT_NOT_FOUND.getMessage()));
    }

    // 구매자 서명
    public Contract signAsBuyer(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.CONTRACT_NOT_FOUND.getMessage()));

        contract.signAsBuyer(); // 엔티티 메서드 사용
        checkIfFullySigned(contract);
        return contractRepository.save(contract);
    }

    // 판매자 서명
    public Contract signAsSeller(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.CONTRACT_NOT_FOUND.getMessage()));

        contract.signAsSeller();
        checkIfFullySigned(contract);
        return contractRepository.save(contract);
    }

    private void checkIfFullySigned(Contract contract) {
        if (contract.isSignedByBuyer() && contract.isSignedBySeller()) {
            Transaction transaction = contract.getTransaction();

            // vehicleId로 직접 조회
            Vehicle vehicle = vehicleRepository.findById(transaction.getVehicleId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

            // 계약서 HTML 생성
            Context context = new Context();
            context.setVariable("transaction", transaction);
            context.setVariable("vehicle", vehicle);
            context.setVariable("contract", contract);

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

                // URL 저장 (정적 리소스 매핑 기준)
                contract.setContractPdfUrl("/contracts/" + fileName);
            } catch (Exception e) {
                throw new InternalServerException(ErrorStatus.PASSPORT_SIGN_ERROR_EXCEPTION.getMessage());
            }

            // Transaction 상태 업데이트
            transaction.setStatus("COMPLETED");
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        }
    }
}
