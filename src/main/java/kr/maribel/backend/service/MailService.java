package kr.maribel.backend.service;

import java.time.Instant;
import java.util.List;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.domain.DomainEnums.MailSourceType;
import kr.maribel.backend.domain.DomainEnums.OutboundMailStatus;
import kr.maribel.backend.domain.MailTemplate;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.OutboundMail;
import kr.maribel.backend.repository.MailTemplateRepository;
import kr.maribel.backend.repository.OutboundMailRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MailService {

    private final OutboundMailRepository outboundMailRepository;
    private final MailTemplateRepository mailTemplateRepository;
    private final MaribelProperties properties;

    public MailService(OutboundMailRepository outboundMailRepository,
                       MailTemplateRepository mailTemplateRepository,
                       MaribelProperties properties) {
        this.outboundMailRepository = outboundMailRepository;
        this.mailTemplateRepository = mailTemplateRepository;
        this.properties = properties;
    }

    @Transactional
    public OutboundMail enqueue(Member member, MailTemplate template, MailSourceType sourceType, String sourceRefId) {
        String idempotencyKey = sourceType + ":" + sourceRefId + ":" + member.getMinecraftUuid() + ":" + template.getMailCode();
        return outboundMailRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> outboundMailRepository.save(new OutboundMail(
                        member.getMinecraftUuid(),
                        template,
                        sourceType,
                        sourceRefId,
                        idempotencyKey
                )));
    }

    @Transactional
    public OutboundMail enqueueManual(String targetUuid, Long mailTemplateId, String sourceRefId) {
        MailTemplate template = mailTemplateRepository.findById(mailTemplateId)
                .orElseThrow(() -> ApiException.notFound("MAIL_TEMPLATE_NOT_FOUND", "우편 템플릿을 찾을 수 없습니다."));
        String idempotencyKey = MailSourceType.ADMIN + ":" + sourceRefId + ":" + targetUuid + ":" + template.getMailCode();
        return outboundMailRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> outboundMailRepository.save(new OutboundMail(
                        targetUuid,
                        template,
                        MailSourceType.ADMIN,
                        sourceRefId,
                        idempotencyKey
                )));
    }

    @Transactional(readOnly = true)
    public List<OutboundMail> listForMember(Member member) {
        return outboundMailRepository.findTop50ByTargetUuidOrderByCreatedAtDesc(member.getMinecraftUuid());
    }

    @Transactional(readOnly = true)
    public List<OutboundMail> listDispatchable(String apiKey, int limit) {
        assertWebpanelKey(apiKey);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return outboundMailRepository.findDispatchable(
                OutboundMailStatus.PENDING,
                OutboundMailStatus.FAILED,
                Instant.now(),
                PageRequest.of(0, boundedLimit)
        );
    }

    @Transactional
    public OutboundMail acknowledge(String apiKey, Long mailId, OutboundMailStatus status, String errorMessage, boolean retryable) {
        assertWebpanelKey(apiKey);
        OutboundMail mail = getMail(mailId);
        if (status == OutboundMailStatus.SENT) {
            mail.markSent();
            return mail;
        }
        if (status == OutboundMailStatus.FAILED) {
            mail.markFailed(StringUtils.hasText(errorMessage) ? errorMessage : "webpanel delivery failed", retryable);
            return mail;
        }
        throw ApiException.badRequest("INVALID_MAIL_ACK_STATUS", "웹패널 ACK는 SENT 또는 FAILED만 허용됩니다.");
    }

    @Transactional
    public OutboundMail retry(Long mailId) {
        OutboundMail mail = getMail(mailId);
        if (mail.getStatus() == OutboundMailStatus.SENT) {
            throw ApiException.badRequest("MAIL_ALREADY_SENT", "이미 발송 완료된 우편입니다.");
        }
        mail.retry();
        return mail;
    }

    @Transactional(readOnly = true)
    public OutboundMail getMail(Long mailId) {
        return outboundMailRepository.findById(mailId)
                .orElseThrow(() -> ApiException.notFound("OUTBOUND_MAIL_NOT_FOUND", "우편 큐 항목을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<OutboundMail> listAllRecent() {
        return outboundMailRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private void assertWebpanelKey(String apiKey) {
        if (!StringUtils.hasText(apiKey) || !apiKey.equals(properties.getWebpanel().getApiKey())) {
            throw ApiException.unauthorized("INVALID_WEBPANEL_KEY", "웹패널 API 키가 올바르지 않습니다.");
        }
    }
}
