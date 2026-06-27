package kr.maribel.backend.service;

import java.util.List;
import kr.maribel.backend.api.ApiDtos.DashboardResponse;
import kr.maribel.backend.domain.AuditLog;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.Category;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import kr.maribel.backend.domain.DomainEnums.ContactStatus;
import kr.maribel.backend.domain.DomainEnums.OutboundMailStatus;
import kr.maribel.backend.domain.DomainEnums.RefundStatus;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import kr.maribel.backend.domain.MailTemplate;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.repository.AuditLogRepository;
import kr.maribel.backend.repository.CashChargeRepository;
import kr.maribel.backend.repository.CategoryRepository;
import kr.maribel.backend.repository.ContactInquiryRepository;
import kr.maribel.backend.repository.MailTemplateRepository;
import kr.maribel.backend.repository.MemberRepository;
import kr.maribel.backend.repository.OutboundMailRepository;
import kr.maribel.backend.repository.RefundRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final CashChargeRepository cashChargeRepository;
    private final OutboundMailRepository outboundMailRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final ContactInquiryRepository contactInquiryRepository;
    private final AuditLogRepository auditLogRepository;
    private final CategoryRepository categoryRepository;
    private final MailTemplateRepository mailTemplateRepository;

    public AdminQueryService(MemberRepository memberRepository,
                             CashChargeRepository cashChargeRepository,
                             OutboundMailRepository outboundMailRepository,
                             RefundRequestRepository refundRequestRepository,
                             ContactInquiryRepository contactInquiryRepository,
                             AuditLogRepository auditLogRepository,
                             CategoryRepository categoryRepository,
                             MailTemplateRepository mailTemplateRepository) {
        this.memberRepository = memberRepository;
        this.cashChargeRepository = cashChargeRepository;
        this.outboundMailRepository = outboundMailRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.contactInquiryRepository = contactInquiryRepository;
        this.auditLogRepository = auditLogRepository;
        this.categoryRepository = categoryRepository;
        this.mailTemplateRepository = mailTemplateRepository;
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
    public Page<Member> members(UserStatus status, String keyword, int page, int size) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : "";
        return memberRepository.search(status, normalizedKeyword, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CashCharge> charges(ChargeStatus status, int page, int size) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return status == null
                ? cashChargeRepository.findAll(pageable)
                : cashChargeRepository.findAllByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> auditLogs(int page, int size) {
        return auditLogRepository.findAll(pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MailTemplate> mailTemplates() {
        return mailTemplateRepository.findAll();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int boundedPage = Math.max(0, page);
        return PageRequest.of(boundedPage, boundedSize, sort);
    }
}
