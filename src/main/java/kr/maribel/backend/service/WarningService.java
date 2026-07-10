package kr.maribel.backend.service;

import java.util.List;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.DomainEnums.MailSourceType;
import kr.maribel.backend.domain.DomainEnums.WarningReason;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.OutboundMail;
import kr.maribel.backend.domain.Warning;
import kr.maribel.backend.repository.OutboundMailRepository;
import kr.maribel.backend.repository.MemberRepository;
import kr.maribel.backend.repository.WarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 유저 경고 부여/취소 및 악성 유저 조회. 경고 3회 이상이면 악성 유저로 분류한다.
 * 경고 부여 시 유형별 안내 메일을 유저 홈페이지 우편함으로 자동 발송한다.
 */
@Service
public class WarningService {

    public static final int MALICIOUS_THRESHOLD = 3;

    private final WarningRepository warningRepository;
    private final MemberRepository memberRepository;
    private final OutboundMailRepository outboundMailRepository;

    public WarningService(WarningRepository warningRepository,
                          MemberRepository memberRepository,
                          OutboundMailRepository outboundMailRepository) {
        this.warningRepository = warningRepository;
        this.memberRepository = memberRepository;
        this.outboundMailRepository = outboundMailRepository;
    }

    @Transactional
    public Warning grant(Member member, WarningReason reason, String detail, String customText, String issuedBy) {
        if (!StringUtils.hasText(detail)) {
            throw ApiException.badRequest("WARNING_DETAIL_REQUIRED", "경고 사유(사건경위)를 입력해 주세요.");
        }
        if (reason == WarningReason.CUSTOM && !StringUtils.hasText(customText)) {
            throw ApiException.badRequest("WARNING_CUSTOM_TEXT_REQUIRED", "직접 작성 경고는 안내 문구를 입력해 주세요.");
        }
        int newCount = (int) warningRepository.countByMemberIdAndCanceledFalse(member.getId()) + 1;
        Warning warning = warningRepository.save(new Warning(
                member,
                reason,
                detail,
                reason == WarningReason.CUSTOM ? customText : null,
                newCount,
                issuedBy
        ));
        member.setWarningCount(newCount);
        sendWarningMail(member, reason, customText, newCount, warning.getId());
        return warning;
    }

    @Transactional
    public Warning cancel(Long warningId, String reason) {
        Warning warning = warningRepository.findById(warningId)
                .orElseThrow(() -> ApiException.notFound("WARNING_NOT_FOUND", "경고를 찾을 수 없습니다."));
        if (!warning.isCanceled()) {
            warning.cancel(reason);
            Member member = warning.getMember();
            member.setWarningCount((int) warningRepository.countByMemberIdAndCanceledFalse(member.getId()));
        }
        return warning;
    }

    @Transactional(readOnly = true)
    public List<Warning> listForMember(Long memberId) {
        return warningRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<Member> maliciousMembers() {
        return memberRepository.findByWarningCountGreaterThanEqualOrderByWarningCountDescCreatedAtDesc(MALICIOUS_THRESHOLD);
    }

    // 경고 안내 메일: 홈페이지 우편함 전용(인게임 디스패치 큐 제외 위해 SENT 로 저장)
    private void sendWarningMail(Member member, WarningReason reason, String customText, int count, Long warningId) {
        OutboundMail mail = new OutboundMail(
                member.getMinecraftUuid(),
                "WARNING_NOTICE",
                WarningMailTemplates.subject(reason),
                WarningMailTemplates.content(reason, customText, count),
                "[]",
                MailSourceType.ADMIN,
                String.valueOf(warningId),
                "WARNING:" + warningId + ":" + member.getMinecraftUuid()
        );
        mail.markSent();
        outboundMailRepository.save(mail);
    }
}
