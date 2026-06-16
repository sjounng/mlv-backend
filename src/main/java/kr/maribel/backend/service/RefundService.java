package kr.maribel.backend.service;

import java.util.List;
import kr.maribel.backend.api.ApiDtos.RefundCreateRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.AdminAccount;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import kr.maribel.backend.domain.DomainEnums.RefundStatus;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.RefundRequest;
import kr.maribel.backend.repository.AdminAccountRepository;
import kr.maribel.backend.repository.CashChargeRepository;
import kr.maribel.backend.repository.RefundRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final CashChargeRepository cashChargeRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final CashService cashService;

    public RefundService(RefundRequestRepository refundRequestRepository,
                         CashChargeRepository cashChargeRepository,
                         AdminAccountRepository adminAccountRepository,
                         CashService cashService) {
        this.refundRequestRepository = refundRequestRepository;
        this.cashChargeRepository = cashChargeRepository;
        this.adminAccountRepository = adminAccountRepository;
        this.cashService = cashService;
    }

    @Transactional
    public RefundRequest create(Member member, RefundCreateRequest request) {
        CashCharge charge = cashChargeRepository.findById(request.cashChargeId())
                .orElseThrow(() -> ApiException.notFound("CASH_CHARGE_NOT_FOUND", "충전 주문을 찾을 수 없습니다."));
        if (!charge.getMember().getId().equals(member.getId())) {
            throw ApiException.forbidden("REFUND_OWNER_MISMATCH", "본인의 결제 건만 환불 요청할 수 있습니다.");
        }
        if (charge.getStatus() != ChargeStatus.PAID) {
            throw ApiException.badRequest("REFUND_NOT_AVAILABLE", "결제 완료 건만 환불 요청할 수 있습니다.");
        }
        return refundRequestRepository.save(new RefundRequest(charge, request.reason()));
    }

    @Transactional(readOnly = true)
    public List<RefundRequest> recent() {
        return refundRequestRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional
    public RefundRequest process(Long refundId, Long adminId, RefundStatus status, String operatorMemo) {
        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> ApiException.notFound("REFUND_NOT_FOUND", "환불 요청을 찾을 수 없습니다."));
        AdminAccount admin = adminAccountRepository.findById(adminId)
                .orElseThrow(() -> ApiException.notFound("ADMIN_NOT_FOUND", "관리자를 찾을 수 없습니다."));

        if (status == RefundStatus.COMPLETED) {
            CashCharge charge = refund.getCashCharge();
            if (charge.getStatus() != ChargeStatus.PAID) {
                throw ApiException.badRequest("REFUND_ALREADY_PROCESSED", "이미 환불 처리되었거나 환불할 수 없는 주문입니다.");
            }
            cashService.deductForRefund(charge.getMember(), charge.getCashAmount(), charge.getMerchantOrderId(), "Cash charge refund");
            charge.markRefunded();
        }

        refund.process(status, admin, operatorMemo);
        return refund;
    }
}
