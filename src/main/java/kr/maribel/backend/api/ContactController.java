package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.InquiryCreateRequest;
import kr.maribel.backend.api.ApiDtos.InquiryResponse;
import kr.maribel.backend.api.ApiDtos.UploadResponse;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.service.ContactService;
import kr.maribel.backend.service.ImageStorageService;
import kr.maribel.backend.service.MemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contact")
@Tag(name = "Contact")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ContactController {

    // 문의 첨부는 png/jpeg 만 허용 (07-08 피드백)
    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final ContactService contactService;
    private final MemberService memberService;
    private final ImageStorageService imageStorageService;

    public ContactController(ContactService contactService, MemberService memberService,
                            ImageStorageService imageStorageService) {
        this.contactService = contactService;
        this.memberService = memberService;
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(value = "/inquiries/attachment", consumes = "multipart/form-data")
    @Operation(summary = "문의 이미지 첨부 업로드 (png/jpeg)")
    UploadResponse uploadAttachment(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                    @RequestParam("file") MultipartFile file) {
        // 인증 사용자만 접근(SecurityConfig anyRequest().authenticated()). 업로드한 URL 을 문의 작성 시 attachmentUrl 로 전달한다.
        return new UploadResponse(imageStorageService.store(file, ATTACHMENT_EXTENSIONS));
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
