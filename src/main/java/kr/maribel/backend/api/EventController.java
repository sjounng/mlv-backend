package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.ClaimResponse;
import kr.maribel.backend.api.ApiDtos.EventResponse;
import kr.maribel.backend.api.ApiDtos.RedeemUseRequest;
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
    @Operation(summary = "진행 중 이벤트 목록 조회")
    List<EventResponse> events() {
        return eventService.activeEvents().stream().map(DtoMapper::event).toList();
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
