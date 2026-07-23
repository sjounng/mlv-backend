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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

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
        // 같은 충전 건에 대해 처리 대기중인 환불 요청이 이미 있으면 중복 접수하지 않는다.
        if (refundRequestRepository.existsByCashChargeIdAndStatus(charge.getId(), RefundStatus.REQUESTED)) {
            throw ApiException.badRequest("REFUND_ALREADY_REQUESTED", "이미 환불 요청이 접수되어 처리 대기 중입니다.");
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
        // 멤버 기반 관리자는 admin_accounts 레코드가 없어 adminId 가 null 이다.
        //  processed_by 는 nullable 이므로 이 경우 처리자를 null 로 기록한다. (점검 M2 — 500 방지)
        AdminAccount admin = adminId == null
                ? null
                : adminAccountRepository.findById(adminId)
                        .orElseThrow(() -> ApiException.notFound("ADMIN_NOT_FOUND", "관리자를 찾을 수 없습니다."));

        // 이미 처리된(REQUESTED 가 아닌) 환불 요청은 재처리하지 않는다. (점검 M1 — 상태 가드)
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw ApiException.badRequest("REFUND_ALREADY_PROCESSED", "이미 처리된 환불 요청입니다.");
        }

        if (status == RefundStatus.COMPLETED) {
            // 동시 처리로 인한 이중 차감을 막기 위해 충전 행을 잠근 뒤 상태를 확인한다. (점검 M1)
            CashCharge charge = cashChargeRepository.findByIdForUpdate(refund.getCashCharge().getId())
                    .orElseThrow(() -> ApiException.notFound("CASH_CHARGE_NOT_FOUND", "충전 주문을 찾을 수 없습니다."));
            if (charge.getStatus() != ChargeStatus.PAID) {
                throw ApiException.badRequest("REFUND_ALREADY_PROCESSED", "이미 환불 처리되었거나 환불할 수 없는 주문입니다.");
            }
            cashService.deductForRefund(charge.getMember(), charge.getCashAmount(), charge.getMerchantOrderId(), "Cash charge refund");
            charge.markRefunded();
            log.info("refund completed: refundId={}, merchantOrderId={}, memberId={}, cashAmount={}, adminId={}",
                    refund.getId(), charge.getMerchantOrderId(), charge.getMember().getId(), charge.getCashAmount(), adminId);
        }

        refund.process(status, admin, operatorMemo);
        return refund;
    }
}
