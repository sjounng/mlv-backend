package kr.maribel.backend.service;

import java.util.List;
import kr.maribel.backend.api.ApiDtos.DashboardResponse;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import kr.maribel.backend.domain.DomainEnums.ContactStatus;
import kr.maribel.backend.domain.DomainEnums.OutboundMailStatus;
import kr.maribel.backend.domain.DomainEnums.RefundStatus;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.repository.CashChargeRepository;
import kr.maribel.backend.repository.ContactInquiryRepository;
import kr.maribel.backend.repository.MemberRepository;
import kr.maribel.backend.repository.OutboundMailRepository;
import kr.maribel.backend.repository.RefundRequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminQueryService {

    private final MemberRepository memberRepository;
    private final CashChargeRepository cashChargeRepository;
    private final OutboundMailRepository outboundMailRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final ContactInquiryRepository contactInquiryRepository;
    public AdminQueryService(MemberRepository memberRepository,
                             CashChargeRepository cashChargeRepository,
                             OutboundMailRepository outboundMailRepository,
                             RefundRequestRepository refundRequestRepository,
                             ContactInquiryRepository contactInquiryRepository) {
        this.memberRepository = memberRepository;
        this.cashChargeRepository = cashChargeRepository;
        this.outboundMailRepository = outboundMailRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.contactInquiryRepository = contactInquiryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                memberRepository.countByStatus(UserStatus.ACTIVE),
                cashChargeRepository.countByStatus(ChargeStatus.PAID),
                outboundMailRepository.countByStatus(OutboundMailStatus.PENDING),
                outboundMailRepository.countByStatus(OutboundMailStatus.FAILED),
                refundRequestRepository.countByStatus(RefundStatus.REQUESTED),
                contactInquiryRepository.countByStatus(ContactStatus.OPEN)
        );
    }

    @Transactional(readOnly = true)
    public List<Member> recentMembers() {
        return memberRepository.findAll(PageRequest.of(0, 100)).getContent();
    }
}
