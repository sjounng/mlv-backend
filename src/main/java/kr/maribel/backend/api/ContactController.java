package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.InquiryCreateRequest;
import kr.maribel.backend.api.ApiDtos.InquiryResponse;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.service.ContactService;
import kr.maribel.backend.service.MemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@Tag(name = "Contact")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ContactController {

    private final ContactService contactService;
    private final MemberService memberService;

    public ContactController(ContactService contactService, MemberService memberService) {
        this.contactService = contactService;
        this.memberService = memberService;
    }

    @PostMapping("/inquiries")
    @Operation(summary = "문의 작성")
    InquiryResponse create(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                           @Valid @RequestBody InquiryCreateRequest request) {
        Member member = memberService.getActiveMember(principal.memberId());
        return DtoMapper.inquiry(contactService.create(member, request));
    }

    @GetMapping("/inquiries/my")
    @Operation(summary = "내 문의 내역 조회")
    List<InquiryResponse> mine(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Member member = memberService.getActiveMember(principal.memberId());
        return contactService.mine(member).stream().map(DtoMapper::inquiry).toList();
    }
}
