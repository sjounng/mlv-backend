package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.AttendanceResponse;
import kr.maribel.backend.api.ApiDtos.ClaimResponse;
import kr.maribel.backend.api.ApiDtos.EventResponse;
import kr.maribel.backend.api.ApiDtos.PageResponse;
import kr.maribel.backend.api.ApiDtos.RedeemUseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestParam;
import kr.maribel.backend.domain.EventParticipation;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.RedeemCodeUsage;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.service.EventService;
import kr.maribel.backend.service.MemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Events")
public class EventController {

    private final EventService eventService;
    private final MemberService memberService;

    public EventController(EventService eventService, MemberService memberService) {
        this.eventService = eventService;
        this.memberService = memberService;
    }

    @GetMapping("/api/events")
    @Operation(summary = "공개 이벤트 목록 조회 (검색/페이지네이션)")
    Object events(@RequestParam(required = false) String q,
                  @RequestParam(required = false) Integer page,
                  @RequestParam(required = false) Integer size) {
        // page/size 가 없으면 배열(구버전 호환), 있으면 페이지네이션 응답.
        if (page == null && size == null) {
            return eventService.publishedEvents(q, PageRequest.of(0, 1000)).getContent()
                    .stream().map(DtoMapper::event).toList();
        }
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 6 : Math.min(50, Math.max(1, size));
        Page<?> result = eventService.publishedEvents(q, PageRequest.of(p, s));
        @SuppressWarnings("unchecked")
        Page<kr.maribel.backend.domain.MaribelEvent> events = (Page<kr.maribel.backend.domain.MaribelEvent>) result;
        return PageResponse.of(events, events.getContent().stream().map(DtoMapper::event).toList());
    }

    @GetMapping("/api/events/featured")
    @Operation(summary = "상단 슬라이더용 featured 이벤트 목록")
    List<EventResponse> featuredEvents() {
        return eventService.featuredEvents().stream().map(DtoMapper::event).toList();
    }

    @GetMapping("/api/events/{id}")
    @Operation(summary = "공개 이벤트 상세 조회")
    EventResponse event(@PathVariable Long id) {
        return DtoMapper.event(eventService.publicEvent(id));
    }

    @GetMapping("/api/events/attendance")
    @Operation(summary = "출석 보드 조회 (이번 달)", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    AttendanceResponse attendance(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Member member = memberService.getActiveMember(principal.memberId());
        return eventService.attendanceBoard(member);
    }

    @PostMapping("/api/events/{id}/claim")
    @Operation(summary = "이벤트 보상 수령", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    ClaimResponse claim(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        Member member = memberService.getActiveMember(principal.memberId());
        EventParticipation participation = eventService.claim(member, id);
        return new ClaimResponse(participation.getId(), DtoMapper.mail(participation.getOutboundMail()));
    }

    @PostMapping("/api/redeem-codes/use")
    @Operation(summary = "리딤코드 사용", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    ClaimResponse useRedeemCode(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                @Valid @RequestBody RedeemUseRequest request) {
        Member member = memberService.getActiveMember(principal.memberId());
        RedeemCodeUsage usage = eventService.useRedeemCode(member, request.code());
        return new ClaimResponse(usage.getId(), DtoMapper.mail(usage.getOutboundMail()));
    }
}
