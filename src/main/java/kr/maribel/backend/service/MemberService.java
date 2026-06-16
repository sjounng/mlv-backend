package kr.maribel.backend.service;

import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Member getActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "회원을 찾을 수 없습니다."));
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden("USER_NOT_ACTIVE", "활성 회원만 사용할 수 있습니다.");
        }
        return member;
    }

    @Transactional
    public void withdraw(Long memberId) {
        Member member = getActiveMember(memberId);
        member.withdraw();
    }

    /** 관리자 조회/제재용. 비활성 회원도 조회한다. */
    @Transactional(readOnly = true)
    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "회원을 찾을 수 없습니다."));
    }

    @Transactional
    public Member suspend(Long memberId) {
        Member member = getMember(memberId);
        try {
            member.suspend();
        } catch (IllegalStateException exception) {
            throw ApiException.badRequest("MEMBER_NOT_SUSPENDABLE", "탈퇴한 회원은 제재할 수 없습니다.");
        }
        return member;
    }

    @Transactional
    public Member reactivate(Long memberId) {
        Member member = getMember(memberId);
        try {
            member.reactivate();
        } catch (IllegalStateException exception) {
            throw ApiException.badRequest("MEMBER_NOT_REACTIVATABLE", "탈퇴한 회원은 복구할 수 없습니다.");
        }
        return member;
    }
}
