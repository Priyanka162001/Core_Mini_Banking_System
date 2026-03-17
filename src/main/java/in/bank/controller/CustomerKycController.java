package in.bank.controller;

import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.KycVerificationStatus;
import in.bank.service.CustomerKycService;
import in.bank.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kyc/v1")
@RequiredArgsConstructor
public class CustomerKycController {

    private final CustomerKycService kycService;
    private final JwtService jwtService;

    @PostMapping("/submit")
    public KycResponseDTO saveKyc(@RequestBody KycRequestDTO request) {
        Long customerId = request.getCustomerId(); // get customerId from request body
        return kycService.saveKycRecordDTO(customerId, request);
    }
    
    // ✅ Get KYC record for a customer
    @GetMapping("/{customerId}")
    public KycResponseDTO getKyc(@PathVariable Long customerId) {
        return kycService.getKycRecordDTO(customerId);
    }

    // ✅ Admin: get all KYC by status
    @GetMapping("/status/{status}")
    public List<KycResponseDTO> getByStatus(@PathVariable KycVerificationStatus status) {
        return kycService.getByStatusDTO(status);
    }

    // ✅ Admin: update KYC status
    @PutMapping("/update-status/{customerId}")
    public KycResponseDTO updateStatus(
            @PathVariable Long customerId,
            @RequestParam KycVerificationStatus status) {

        return kycService.updateKycStatusDTO(customerId, status);
    }
}