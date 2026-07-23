package kr.maribel.backend.service;

import java.util.List;
import kr.maribel.backend.api.ApiDtos.InquiryCreateRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.AdminAccount;
import kr.maribel.backend.domain.ContactInquiry;
import kr.maribel.backend.domain.InquiryReply;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.repository.AdminAccountRepository;
import kr.maribel.backend.repository.ContactInquiryRepository;
import kr.maribel.backend.repository.InquiryReplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private final ContactInquiryRepository inquiryRepository;
    private final InquiryReplyRepository replyRepository;
    private final AdminAccountRepository adminAccountRepository;

    public ContactService(ContactInquiryRepository inquiryRepository,
                          InquiryReplyRepository replyRepository,
                          AdminAccountRepository adminAccountRepository) {
        this.inquiryRepository = inquiryRepository;
        this.replyRepository = replyRepository;
        this.adminAccountRepository = adminAccountRepository;
    }

    @Transactional
    public ContactInquiry create(Member member, InquiryCreateRequest request) {
        return inquiryRepository.save(new ContactInquiry(
                member,
                request.category(),
                request.title(),
                request.content(),
                request.attachmentUrl()
        ));
    }

    @Transactional(readOnly = true)
    public List<ContactInquiry> mine(Member member) {
        return inquiryRepository.findTop50ByMemberIdOrderByCreatedAtDesc(member.getId());
    }

    @Transactional(readOnly = true)
    public List<ContactInquiry> recent() {
        return inquiryRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional
    public ContactInquiry reply(Long inquiryId, Long adminId, String content) {
        ContactInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다."));
        // 멤버 기반 관리자는 admin_accounts 레코드가 없어 adminId 가 null 이다. (점검 M2 — 500 방지)
        AdminAccount admin = adminId == null
                ? null
                : adminAccountRepository.findById(adminId)
                        .orElseThrow(() -> ApiException.notFound("ADMIN_NOT_FOUND", "관리자를 찾을 수 없습니다."));
        replyRepository.save(new InquiryReply(inquiry, admin, content));
        inquiry.markAnswered();
        return inquiry;
    }
}
