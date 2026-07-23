package kr.maribel.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import kr.maribel.backend.api.ApiDtos.AttendanceResponse;
import kr.maribel.backend.api.ApiDtos.EventUpsertRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.DomainEnums.EventType;
import kr.maribel.backend.domain.DomainEnums.MailSourceType;
import kr.maribel.backend.domain.EventParticipation;
import kr.maribel.backend.domain.MailTemplate;
import kr.maribel.backend.domain.MaribelEvent;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.OutboundMail;
import kr.maribel.backend.domain.RedeemCode;
import kr.maribel.backend.domain.RedeemCodeUsage;
import kr.maribel.backend.repository.EventParticipationRepository;
import kr.maribel.backend.repository.MailTemplateRepository;
import kr.maribel.backend.repository.MaribelEventRepository;
import kr.maribel.backend.repository.RedeemCodeRepository;
import kr.maribel.backend.repository.RedeemCodeUsageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MaribelEventRepository eventRepository;
    private final EventParticipationRepository participationRepository;
    private final RedeemCodeRepository redeemCodeRepository;
    private final RedeemCodeUsageRepository redeemCodeUsageRepository;
    private final MailTemplateRepository mailTemplateRepository;
    private final MailService mailService;

    public EventService(MaribelEventRepository eventRepository,
                        EventParticipationRepository participationRepository,
                        RedeemCodeRepository redeemCodeRepository,
                        RedeemCodeUsageRepository redeemCodeUsageRepository,
                        MailTemplateRepository mailTemplateRepository,
                        MailService mailService) {
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.redeemCodeRepository = redeemCodeRepository;
        this.redeemCodeUsageRepository = redeemCodeUsageRepository;
        this.mailTemplateRepository = mailTemplateRepository;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public List<MaribelEvent> activeEvents() {
        return eventRepository.findActiveEvents(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<MaribelEvent> allEvents() {
        return eventRepository.findAllByOrderByStartAtDesc();
    }

    // 공개 이벤트 목록 — 게시된 것 전체를 게시일 최신순으로, 제목 검색(띄어쓰기 무관 다중 키워드) 후 페이지네이션.
    //  이벤트 수가 많지 않아 서비스 레벨에서 필터/페이징한다. (07-22 목록형 개편)
    @Transactional(readOnly = true)
    public Page<MaribelEvent> publishedEvents(String query, Pageable pageable) {
        List<MaribelEvent> all = eventRepository.findByActiveTrueOrderByCreatedAtDesc();
        List<MaribelEvent> filtered = applySearch(all, query);
        int from = (int) Math.min(pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(from, to), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public List<MaribelEvent> featuredEvents() {
        return eventRepository.findByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public MaribelEvent publicEvent(Long id) {
        return eventRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ApiException.notFound("EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."));
    }

    // 제목 정규화(공백 제거, 소문자) 후, 검색어의 각 토큰이 모두 포함되는 이벤트만 남긴다.
    //  "핫타임 접속 보상" / "핫타임접속보상" 모두 동일하게 매칭.
    private List<MaribelEvent> applySearch(List<MaribelEvent> events, String query) {
        if (query == null || query.isBlank()) {
            return events;
        }
        String[] tokens = query.trim().toLowerCase().split("\\s+");
        return events.stream()
                .filter(e -> {
                    String normalized = e.getName().toLowerCase().replaceAll("\\s+", "");
                    for (String token : tokens) {
                        if (!normalized.contains(token)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceResponse attendanceBoard(Member member) {
        Instant now = Instant.now();
        String today = LocalDate.now(SERVICE_ZONE).toString();
        Optional<MaribelEvent> attendanceEvent = eventRepository.findActiveEvents(now).stream()
                .filter(e -> e.getType() == EventType.ATTENDANCE)
                .findFirst();
        if (attendanceEvent.isEmpty()) {
            return new AttendanceResponse(null, null, today, false, List.of(), false);
        }
        MaribelEvent event = attendanceEvent.get();
        String monthPrefix = today.substring(0, 7); // YYYY-MM
        List<String> claimedThisMonth = participationRepository.findClaimKeys(member.getId(), event.getId()).stream()
                .filter(key -> key.startsWith(monthPrefix))
                .sorted()
                .toList();
        boolean todayClaimed = claimedThisMonth.contains(today);
        return new AttendanceResponse(
                event.getId(),
                event.getName(),
                today,
                todayClaimed,
                claimedThisMonth,
                event.isClaimable(now) && !todayClaimed
        );
    }

    @Transactional
    public EventParticipation claim(Member member, Long eventId) {
        MaribelEvent event = eventRepository.findWithMailTemplateById(eventId)
                .orElseThrow(() -> ApiException.notFound("EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."));
        if (!event.isClaimable(Instant.now())) {
            throw ApiException.badRequest("EVENT_NOT_CLAIMABLE", "현재 수령 가능한 이벤트가 아닙니다.");
        }

        String claimKey = event.getType() == EventType.ATTENDANCE
                ? LocalDate.now(SERVICE_ZONE).toString()
                : "once";
        if (participationRepository.existsByMemberIdAndEventIdAndClaimKey(member.getId(), event.getId(), claimKey)) {
            throw ApiException.conflict("EVENT_ALREADY_CLAIMED", "이미 수령한 이벤트 보상입니다.");
        }

        EventParticipation participation = participationRepository.save(new EventParticipation(member, event, claimKey));
        OutboundMail mail = mailService.enqueue(member, event.getMailTemplate(), MailSourceType.EVENT, event.getId() + ":" + claimKey);
        participation.attachMail(mail);
        return participation;
    }

    @Transactional
    public RedeemCodeUsage useRedeemCode(Member member, String rawCode) {
        RedeemCode code = redeemCodeRepository.findByCodeIgnoreCase(rawCode.trim())
                .orElseThrow(() -> ApiException.notFound("REDEEM_CODE_NOT_FOUND", "리딤코드를 찾을 수 없습니다."));
        if (!code.isUsable(Instant.now())) {
            throw ApiException.badRequest("REDEEM_CODE_NOT_USABLE", "사용할 수 없는 리딤코드입니다.");
        }
        if (redeemCodeUsageRepository.existsByRedeemCodeIdAndMemberId(code.getId(), member.getId())) {
            throw ApiException.conflict("REDEEM_CODE_ALREADY_USED", "이미 사용한 리딤코드입니다.");
        }

        code.increaseUsedCount();
        RedeemCodeUsage usage = redeemCodeUsageRepository.save(new RedeemCodeUsage(code, member));
        OutboundMail mail = mailService.enqueue(member, code.getMailTemplate(), MailSourceType.REDEEM_CODE, code.getCode());
        usage.attachMail(mail);
        return usage;
    }

    @Transactional
    public MaribelEvent upsertEvent(Long id, EventUpsertRequest request) {
        if (request.endAt().isBefore(request.startAt())) {
            throw ApiException.badRequest("INVALID_EVENT_PERIOD", "이벤트 종료일은 시작일 이후여야 합니다.");
        }
        // 보상 템플릿은 선택 사항 (콘텐츠형 이벤트는 연결하지 않음).
        MailTemplate template = request.mailTemplateId() == null ? null
                : mailTemplateRepository.findById(request.mailTemplateId())
                        .orElseThrow(() -> ApiException.notFound("MAIL_TEMPLATE_NOT_FOUND", "우편 템플릿을 찾을 수 없습니다."));
        if (id == null) {
            MaribelEvent event = new MaribelEvent(request.name(), request.type(), request.bannerImageUrl(),
                    request.description(), request.startAt(), request.endAt(), request.status(),
                    request.featured(), template, request.active());
            return eventRepository.save(event);
        }
        MaribelEvent event = eventRepository.findWithMailTemplateById(id)
                .orElseThrow(() -> ApiException.notFound("EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."));
        event.update(request.name(), request.type(), request.bannerImageUrl(), request.description(),
                request.startAt(), request.endAt(), request.status(), request.featured(), template, request.active());
        return event;
    }

    @Transactional
    public void deleteEvent(Long id) {
        MaribelEvent event = eventRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."));
        // 참여(수령) 이력이 있는 이벤트는 이력 보존을 위해 삭제 대신 예외.
        if (participationRepository.countByEventId(id) > 0) {
            throw ApiException.badRequest("EVENT_HAS_PARTICIPATION", "참여 이력이 있는 이벤트는 삭제할 수 없습니다. 비공개로 전환해 주세요.");
        }
        eventRepository.delete(event);
    }

    @Transactional
    public RedeemCode createRedeemCode(String code, Long mailTemplateId, int maxUses, Instant expiresAt) {
        MailTemplate template = mailTemplateRepository.findById(mailTemplateId)
                .orElseThrow(() -> ApiException.notFound("MAIL_TEMPLATE_NOT_FOUND", "우편 템플릿을 찾을 수 없습니다."));
        return redeemCodeRepository.save(new RedeemCode(code, template, maxUses, expiresAt));
    }
}
