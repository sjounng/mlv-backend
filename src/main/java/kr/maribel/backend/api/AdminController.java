package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.AdminMailSendRequest;
import kr.maribel.backend.api.ApiDtos.AdminMemberResponse;
import kr.maribel.backend.api.ApiDtos.AuditLogResponse;
import kr.maribel.backend.api.ApiDtos.CategoryRequest;
import kr.maribel.backend.api.ApiDtos.CategoryResponse;
import kr.maribel.backend.api.ApiDtos.ChargeHistoryResponse;
import kr.maribel.backend.api.ApiDtos.DashboardResponse;
import kr.maribel.backend.api.ApiDtos.EventResponse;
import kr.maribel.backend.api.ApiDtos.EventUpsertRequest;
import kr.maribel.backend.api.ApiDtos.InquiryReplyRequest;
import kr.maribel.backend.api.ApiDtos.InquiryResponse;
import kr.maribel.backend.api.ApiDtos.MailResponse;
import kr.maribel.backend.api.ApiDtos.MailTemplateRequest;
import kr.maribel.backend.api.ApiDtos.MailTemplateResponse;
import kr.maribel.backend.api.ApiDtos.NoticeRequest;
import kr.maribel.backend.api.ApiDtos.NoticeResponse;
import kr.maribel.backend.api.ApiDtos.PageResponse;
import kr.maribel.backend.api.ApiDtos.PopupRequest;
import kr.maribel.backend.api.ApiDtos.PopupResponse;
import kr.maribel.backend.api.ApiDtos.ProductResponse;
import kr.maribel.backend.api.ApiDtos.ProductUpsertRequest;
import kr.maribel.backend.api.ApiDtos.RedeemCodeCreateRequest;
import kr.maribel.backend.api.ApiDtos.RedeemCodeResponse;
import kr.maribel.backend.api.ApiDtos.RefundProcessRequest;
import kr.maribel.backend.api.ApiDtos.RefundResponse;
import kr.maribel.backend.api.ApiDtos.TermsCreateRequest;
import kr.maribel.backend.api.ApiDtos.TermsResponse;
import kr.maribel.backend.domain.AuditLog;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.Category;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import kr.maribel.backend.domain.DomainEnums.Role;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import kr.maribel.backend.domain.MailTemplate;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.OutboundMail;
import kr.maribel.backend.domain.Product;
import kr.maribel.backend.repository.CashChargeRepository;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.service.AdminQueryService;
import kr.maribel.backend.service.AuditService;
import kr.maribel.backend.service.ContactService;
import kr.maribel.backend.service.EventService;
import kr.maribel.backend.service.MailService;
import kr.maribel.backend.service.MemberService;
import kr.maribel.backend.service.NoticeService;
import kr.maribel.backend.service.RefundService;
import kr.maribel.backend.service.LegalService;
import kr.maribel.backend.service.PopupService;
import kr.maribel.backend.service.CashProductService;
import kr.maribel.backend.service.ShopService;
import kr.maribel.backend.service.SiteSettingService;
import kr.maribel.backend.service.WarningService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminController {

    private final AdminQueryService adminQueryService;
    private final ShopService shopService;
    private final EventService eventService;
    private final MailService mailService;
    private final RefundService refundService;
    private final ContactService contactService;
    private final AuditService auditService;
    private final MemberService memberService;
    private final PopupService popupService;
    private final LegalService legalService;
    private final NoticeService noticeService;
    private final WarningService warningService;
    private final CashChargeRepository cashChargeRepository;
    private final SiteSettingService siteSettingService;
    private final CashProductService cashProductService;

    public AdminController(AdminQueryService adminQueryService,
                           ShopService shopService,
                           EventService eventService,
                           MailService mailService,
                           RefundService refundService,
                           ContactService contactService,
                           AuditService auditService,
                           MemberService memberService,
                           PopupService popupService,
                           LegalService legalService,
                           NoticeService noticeService,
                           WarningService warningService,
                           CashChargeRepository cashChargeRepository,
                           SiteSettingService siteSettingService,
                           CashProductService cashProductService) {
        this.adminQueryService = adminQueryService;
        this.shopService = shopService;
        this.eventService = eventService;
        this.mailService = mailService;
        this.refundService = refundService;
        this.contactService = contactService;
        this.auditService = auditService;
        this.memberService = memberService;
        this.popupService = popupService;
        this.legalService = legalService;
        this.noticeService = noticeService;
        this.warningService = warningService;
        this.cashChargeRepository = cashChargeRepository;
        this.siteSettingService = siteSettingService;
        this.cashProductService = cashProductService;
    }

    @GetMapping("/shop-status")
    @Operation(summary = "웹상점 활성화 상태 조회 (관리자)")
    ApiDtos.ShopStatusResponse shopStatus() {
        return new ApiDtos.ShopStatusResponse(siteSettingService.isShopEnabled());
    }

    @PatchMapping("/shop-status")
    @Operation(summary = "웹상점 활성화/비활성화 (07-10 피드백)")
    ApiDtos.ShopStatusResponse updateShopStatus(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                @RequestBody ApiDtos.ShopStatusUpdateRequest request) {
        boolean enabled = siteSettingService.setShopEnabled(request.enabled());
        auditService.record(principal, "SiteSetting", "shop.enabled", "UPDATE", null, String.valueOf(enabled));
        return new ApiDtos.ShopStatusResponse(enabled);
    }

    // ── 캐시 충전 상품 (07-22 웹상점 개편) ──
    @GetMapping("/cash-products")
    @Operation(summary = "캐시 충전 상품 목록 (관리자)")
    List<ApiDtos.CashProductResponse> cashProducts() {
        return cashProductService.allProducts().stream().map(DtoMapper::cashProduct).toList();
    }

    @PostMapping("/cash-products")
    @Operation(summary = "캐시 충전 상품 생성")
    ApiDtos.CashProductResponse createCashProduct(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                  @Valid @RequestBody ApiDtos.CashProductUpsertRequest request) {
        var product = cashProductService.upsert(null, request);
        auditService.record(principal, "CashProduct", String.valueOf(product.getId()), "CREATE", null, product.getName());
        return DtoMapper.cashProduct(product);
    }

    @PatchMapping("/cash-products/{id}")
    @Operation(summary = "캐시 충전 상품 수정")
    ApiDtos.CashProductResponse updateCashProduct(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody ApiDtos.CashProductUpsertRequest request) {
        var product = cashProductService.upsert(id, request);
        auditService.record(principal, "CashProduct", String.valueOf(product.getId()), "UPDATE", null, product.getName());
        return DtoMapper.cashProduct(product);
    }

    @DeleteMapping("/cash-products/{id}")
    @Operation(summary = "캐시 충전 상품 삭제")
    void deleteCashProduct(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        cashProductService.delete(id);
        auditService.record(principal, "CashProduct", String.valueOf(id), "DELETE", null, null);
    }

    @GetMapping("/cash-product-description")
    @Operation(summary = "캐시 충전 상품 공통 상세 소개 조회")
    ApiDtos.CashProductDescriptionResponse cashProductDescription() {
        return new ApiDtos.CashProductDescriptionResponse(siteSettingService.getCashProductDescription());
    }

    @PutMapping("/cash-product-description")
    @Operation(summary = "캐시 충전 상품 공통 상세 소개 설정")
    ApiDtos.CashProductDescriptionResponse updateCashProductDescription(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                                        @RequestBody ApiDtos.CashProductDescriptionRequest request) {
        String value = siteSettingService.setCashProductDescription(request.description());
        auditService.record(principal, "SiteSetting", "cash.product.description", "UPDATE", null, null);
        return new ApiDtos.CashProductDescriptionResponse(value);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "관리자 대시보드 조회")
    DashboardResponse dashboard() {
        return adminQueryService.dashboard();
    }

    @GetMapping("/me")
    @Operation(summary = "현재 로그인한 관리자 정보 (권한 관리 UI 노출 판단)")
    ApiDtos.AdminMeResponse me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return new ApiDtos.AdminMeResponse(principal.memberId(), principal.displayName(), principal.role());
    }

    @GetMapping("/members")
    @Operation(summary = "회원 목록 검색 (상태/키워드 필터 + 페이지네이션)")
    PageResponse<AdminMemberResponse> members(@RequestParam(required = false) UserStatus status,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Page<Member> members = adminQueryService.members(status, keyword, page, size);
        return PageResponse.of(members, members.getContent().stream().map(DtoMapper::adminMember).toList());
    }

    @PatchMapping("/members/{id}/suspend")
    @Operation(summary = "회원 제재 (일시정지)")
    AdminMemberResponse suspendMember(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                      @PathVariable Long id) {
        Member member = memberService.suspend(id);
        auditService.record(principal, "Member", String.valueOf(member.getId()), "SUSPEND", null, member.getStatus().name());
        return DtoMapper.adminMember(member);
    }

    @PatchMapping("/members/{id}/activate")
    @Operation(summary = "회원 제재 해제 (활성화)")
    AdminMemberResponse activateMember(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                       @PathVariable Long id) {
        Member member = memberService.reactivate(id);
        auditService.record(principal, "Member", String.valueOf(member.getId()), "ACTIVATE", null, member.getStatus().name());
        return DtoMapper.adminMember(member);
    }

    // ─── 경고 시스템 (07-09 피드백) ───

    @GetMapping("/members/malicious")
    @Operation(summary = "악성 유저(경고 3회 이상) 일괄 조회")
    List<ApiDtos.MaliciousMemberResponse> maliciousMembers() {
        return warningService.maliciousMembers().stream()
                .map(m -> new ApiDtos.MaliciousMemberResponse(
                        m.getId(), m.getMinecraftUuid(), m.getMinecraftUsername(), m.getEmail(), m.getWarningCount(),
                        warningService.listForMember(m.getId()).stream()
                                .filter(w -> !w.isCanceled()).map(DtoMapper::warning).toList()))
                .toList();
    }

    @GetMapping("/members/{id}")
    @Operation(summary = "회원 통합 조회 (프로필 + 후원금액 + 경고 이력)")
    ApiDtos.AdminMemberDetailResponse memberDetail(@PathVariable Long id) {
        Member member = memberService.getMember(id);
        long totalPaid = cashChargeRepository.sumPaidKrwByMemberId(member.getId());
        List<ApiDtos.WarningResponse> warnings = warningService.listForMember(member.getId())
                .stream().map(DtoMapper::warning).toList();
        return new ApiDtos.AdminMemberDetailResponse(
                member.getId(), member.getMinecraftUuid(), member.getMinecraftUsername(), member.getEmail(),
                null, member.getStatus(), member.getRole(), member.getWarningCount(), totalPaid, member.getCreatedAt(), warnings);
    }

    @PatchMapping("/members/{id}/role")
    @Operation(summary = "회원 권한(역할) 변경 — 최고 관리자 전용")
    AdminMemberResponse changeRole(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody ApiDtos.RoleChangeRequest request) {
        // 자기 자신의 권한은 변경 불가 (실수로 인한 셀프 잠금 방지)
        if (principal.memberId() != null && principal.memberId().equals(id)) {
            throw ApiException.badRequest("CANNOT_CHANGE_OWN_ROLE", "본인의 권한은 변경할 수 없습니다.");
        }
        var before = memberService.getMember(id).getRole();
        // 슈퍼 관리자는 DB 로만 관리한다: 대상이 슈퍼 관리자이거나, 슈퍼 관리자로 승격하려는 요청은 거부.
        if (before == Role.SUPER_ADMIN) {
            throw ApiException.badRequest("CANNOT_MODIFY_SUPER_ADMIN", "슈퍼 관리자의 권한은 대시보드에서 변경할 수 없습니다. (DB 에서만 관리)");
        }
        if (request.role() == Role.SUPER_ADMIN) {
            throw ApiException.badRequest("CANNOT_GRANT_SUPER_ADMIN", "슈퍼 관리자 권한은 대시보드에서 부여할 수 없습니다. (DB 에서만 지정)");
        }
        Member member = memberService.changeRole(id, request.role());
        auditService.record(principal, "Member", String.valueOf(member.getId()), "ROLE_CHANGE",
                before.name(), request.role().name());
        return DtoMapper.adminMember(member);
    }

    @PostMapping("/members/{id}/warnings")
    @Operation(summary = "회원에게 경고 부여 (사유 유형 + 사건경위, 안내 메일 자동 발송)")
    ApiDtos.WarningResponse grantWarning(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                         @PathVariable Long id,
                                         @Valid @RequestBody ApiDtos.WarningGrantRequest request) {
        Member member = memberService.getMember(id);
        var warning = warningService.grant(member, request.reason(), request.detail(), request.customText(), principal.displayName());
        auditService.record(principal, "Warning", String.valueOf(warning.getId()), "GRANT", null,
                request.reason().name() + " / " + member.getMinecraftUsername());
        return DtoMapper.warning(warning);
    }

    @PostMapping("/warnings/{warningId}/cancel")
    @Operation(summary = "경고 취소 (취소 시 누적 경고 수 재계산)")
    ApiDtos.WarningResponse cancelWarning(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                          @PathVariable Long warningId,
                                          @Valid @RequestBody ApiDtos.WarningCancelRequest request) {
        var warning = warningService.cancel(warningId, request.reason());
        auditService.record(principal, "Warning", String.valueOf(warning.getId()), "CANCEL", null, request.reason());
        return DtoMapper.warning(warning);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "감사 로그 조회 (페이지네이션)")
    PageResponse<AuditLogResponse> auditLogs(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logs = adminQueryService.auditLogs(page, size);
        return PageResponse.of(logs, logs.getContent().stream().map(DtoMapper::auditLog).toList());
    }

    @GetMapping("/categories")
    @Operation(summary = "관리자 카테고리 목록 조회")
    List<CategoryResponse> categories() {
        return adminQueryService.categories().stream().map(DtoMapper::category).toList();
    }

    @PostMapping("/categories")
    @Operation(summary = "카테고리 생성")
    CategoryResponse createCategory(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                    @Valid @RequestBody CategoryRequest request) {
        Category category = shopService.upsertCategory(null, request.name(), request.sortOrder(), request.active());
        auditService.record(principal, "Category", String.valueOf(category.getId()), "CREATE", null, category.getName());
        return DtoMapper.category(category);
    }

    @PatchMapping("/categories/{id}")
    @Operation(summary = "카테고리 수정")
    CategoryResponse updateCategory(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody CategoryRequest request) {
        Category category = shopService.upsertCategory(id, request.name(), request.sortOrder(), request.active());
        auditService.record(principal, "Category", String.valueOf(category.getId()), "UPDATE", null, category.getName());
        return DtoMapper.category(category);
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "카테고리 삭제")
    void deleteCategory(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        shopService.deleteCategory(id);
        auditService.record(principal, "Category", String.valueOf(id), "DELETE", null, null);
    }

    @GetMapping("/mail-templates")
    @Operation(summary = "우편 템플릿 목록 조회")
    List<MailTemplateResponse> mailTemplates() {
        return adminQueryService.mailTemplates().stream().map(DtoMapper::mailTemplate).toList();
    }

    @PostMapping("/mail-templates")
    @Operation(summary = "우편 템플릿 생성")
    MailTemplateResponse createMailTemplate(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                            @Valid @RequestBody MailTemplateRequest request) {
        MailTemplate template = shopService.createMailTemplate(request.mailCode(), request.subject(), request.content(), request.rewardsJson());
        auditService.record(principal, "MailTemplate", String.valueOf(template.getId()), "CREATE", null, template.getMailCode());
        return DtoMapper.mailTemplate(template);
    }

    @GetMapping("/products")
    @Operation(summary = "관리자 상품 목록 검색")
    List<ProductResponse> products(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Boolean recommended,
                                   @RequestParam(required = false) Boolean newBadge,
                                   @RequestParam(required = false) Long minPrice,
                                   @RequestParam(required = false) Long maxPrice,
                                   @RequestParam(required = false) String keyword) {
        return shopService.searchProducts(categoryId, recommended, newBadge, minPrice, maxPrice, keyword, false, Pageable.unpaged())
                .stream()
                .map(DtoMapper::product)
                .toList();
    }

    @PostMapping("/products")
    @Operation(summary = "상품 생성")
    ProductResponse createProduct(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                  @Valid @RequestBody ProductUpsertRequest request) {
        Product product = shopService.upsertProduct(null, request);
        auditService.record(principal, "Product", String.valueOf(product.getId()), "CREATE", null, product.getName());
        return DtoMapper.product(product);
    }

    @PatchMapping("/products/{id}")
    @Operation(summary = "상품 수정")
    ProductResponse updateProduct(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ProductUpsertRequest request) {
        Product product = shopService.upsertProduct(id, request);
        auditService.record(principal, "Product", String.valueOf(product.getId()), "UPDATE", null, product.getName());
        return DtoMapper.product(product);
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "상품 삭제")
    void deleteProduct(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        shopService.deleteProduct(id);
        auditService.record(principal, "Product", String.valueOf(id), "DELETE", null, null);
    }

    @GetMapping("/events")
    @Operation(summary = "전체 이벤트 목록 조회")
    List<EventResponse> events() {
        return eventService.allEvents().stream().map(DtoMapper::event).toList();
    }

    @PostMapping("/events")
    @Operation(summary = "이벤트 생성")
    EventResponse createEvent(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                              @Valid @RequestBody EventUpsertRequest request) {
        var event = eventService.upsertEvent(null, request);
        auditService.record(principal, "Event", String.valueOf(event.getId()), "CREATE", null, event.getName());
        return DtoMapper.event(event);
    }

    @PatchMapping("/events/{id}")
    @Operation(summary = "이벤트 수정")
    EventResponse updateEvent(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                              @PathVariable Long id,
                              @Valid @RequestBody EventUpsertRequest request) {
        var event = eventService.upsertEvent(id, request);
        auditService.record(principal, "Event", String.valueOf(event.getId()), "UPDATE", null, event.getName());
        return DtoMapper.event(event);
    }

    @DeleteMapping("/events/{id}")
    @Operation(summary = "이벤트 삭제")
    void deleteEvent(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        eventService.deleteEvent(id);
        auditService.record(principal, "Event", String.valueOf(id), "DELETE", null, null);
    }

    @PostMapping("/redeem-codes")
    @Operation(summary = "리딤코드 생성")
    RedeemCodeResponse createRedeemCode(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                        @Valid @RequestBody RedeemCodeCreateRequest request) {
        var code = eventService.createRedeemCode(request.code(), request.mailTemplateId(), request.maxUses(), request.expiresAt());
        auditService.record(principal, "RedeemCode", String.valueOf(code.getId()), "CREATE", null, code.getCode());
        return DtoMapper.redeemCode(code);
    }

    @GetMapping("/mails")
    @Operation(summary = "최근 우편 큐 조회")
    List<MailResponse> mails() {
        return mailService.listAllRecent().stream().map(DtoMapper::mail).toList();
    }

    @PostMapping("/mails/send")
    @Operation(summary = "운영자 우편 별도 지급")
    MailResponse sendMail(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                          @Valid @RequestBody AdminMailSendRequest request) {
        OutboundMail mail = mailService.enqueueManual(request.targetUuid(), request.mailTemplateId(), request.sourceRefId());
        auditService.record(principal, "OutboundMail", String.valueOf(mail.getId()), "CREATE", null, mail.getMailCode());
        return DtoMapper.mail(mail);
    }

    @PostMapping("/mails/{id}/retry")
    @Operation(summary = "실패 우편 재시도")
    MailResponse retryMail(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                           @PathVariable Long id) {
        OutboundMail mail = mailService.retry(id);
        auditService.record(principal, "OutboundMail", String.valueOf(mail.getId()), "RETRY", null, mail.getStatus().name());
        return DtoMapper.mail(mail);
    }

    @GetMapping("/payments/charges")
    @Operation(summary = "결제 충전 내역 조회 (상태 필터 + 페이지네이션)")
    PageResponse<ChargeHistoryResponse> charges(@RequestParam(required = false) ChargeStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        Page<CashCharge> charges = adminQueryService.charges(status, page, size);
        return PageResponse.of(charges, charges.getContent().stream().map(DtoMapper::chargeHistory).toList());
    }

    @GetMapping("/refunds")
    @Operation(summary = "환불 요청 목록 조회")
    List<RefundResponse> refunds() {
        return refundService.recent().stream().map(DtoMapper::refund).toList();
    }

    @PatchMapping("/refunds/{id}/process")
    @Operation(summary = "환불 요청 처리")
    RefundResponse processRefund(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody RefundProcessRequest request) {
        var refund = refundService.process(id, principal.adminId(), request.status(), request.operatorMemo());
        auditService.record(principal, "RefundRequest", String.valueOf(refund.getId()), "PROCESS", null, refund.getStatus().name());
        return DtoMapper.refund(refund);
    }

    @GetMapping("/inquiries")
    @Operation(summary = "문의 목록 조회")
    List<InquiryResponse> inquiries() {
        return contactService.recent().stream().map(DtoMapper::inquiry).toList();
    }

    @PostMapping("/inquiries/{id}/reply")
    @Operation(summary = "문의 답변 등록")
    InquiryResponse reply(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                          @PathVariable Long id,
                          @Valid @RequestBody InquiryReplyRequest request) {
        var inquiry = contactService.reply(id, principal.adminId(), request.content());
        auditService.record(principal, "ContactInquiry", String.valueOf(inquiry.getId()), "REPLY", null, inquiry.getStatus().name());
        return DtoMapper.inquiry(inquiry);
    }

    @GetMapping("/popups")
    @Operation(summary = "팝업/배너 목록 조회")
    List<PopupResponse> popups() {
        return popupService.all().stream().map(DtoMapper::popup).toList();
    }

    @PostMapping("/popups")
    @Operation(summary = "팝업/배너 등록")
    PopupResponse createPopup(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                              @Valid @RequestBody PopupRequest request) {
        var popup = popupService.create(request);
        auditService.record(principal, "Popup", String.valueOf(popup.getId()), "CREATE", null, popup.getImageUrl());
        return DtoMapper.popup(popup);
    }

    @PatchMapping("/popups/{id}")
    @Operation(summary = "팝업/배너 수정")
    PopupResponse updatePopup(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                              @PathVariable Long id,
                              @Valid @RequestBody PopupRequest request) {
        var popup = popupService.update(id, request);
        auditService.record(principal, "Popup", String.valueOf(popup.getId()), "UPDATE", null, popup.getImageUrl());
        return DtoMapper.popup(popup);
    }

    @PatchMapping("/popups/{id}/active")
    @Operation(summary = "팝업/배너 노출 토글")
    PopupResponse setPopupActive(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                 @PathVariable Long id,
                                 @RequestParam boolean active) {
        var popup = popupService.setActive(id, active);
        auditService.record(principal, "Popup", String.valueOf(popup.getId()), "SET_ACTIVE", null, String.valueOf(active));
        return DtoMapper.popup(popup);
    }

    @GetMapping("/notices")
    @Operation(summary = "공지사항 목록 조회")
    List<NoticeResponse> notices() {
        return noticeService.all().stream().map(DtoMapper::notice).toList();
    }

    @PostMapping("/notices")
    @Operation(summary = "공지사항 등록")
    NoticeResponse createNotice(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                @Valid @RequestBody NoticeRequest request) {
        var notice = noticeService.create(request);
        auditService.record(principal, "Notice", String.valueOf(notice.getId()), "CREATE", null, notice.getTitle());
        return DtoMapper.notice(notice);
    }

    @PatchMapping("/notices/{id}")
    @Operation(summary = "공지사항 수정")
    NoticeResponse updateNotice(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                @PathVariable Long id,
                                @Valid @RequestBody NoticeRequest request) {
        var notice = noticeService.update(id, request);
        auditService.record(principal, "Notice", String.valueOf(notice.getId()), "UPDATE", null, notice.getTitle());
        return DtoMapper.notice(notice);
    }

    @DeleteMapping("/notices/{id}")
    @Operation(summary = "공지사항 삭제")
    void deleteNotice(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        noticeService.delete(id);
        auditService.record(principal, "Notice", String.valueOf(id), "DELETE", null, null);
    }

    @GetMapping("/terms")
    @Operation(summary = "약관/방침 전체 버전 조회")
    List<TermsResponse> terms() {
        return legalService.all().stream().map(DtoMapper::terms).toList();
    }

    @PostMapping("/terms")
    @Operation(summary = "약관/방침 새 버전 게시")
    TermsResponse publishTerms(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                               @Valid @RequestBody TermsCreateRequest request) {
        var terms = legalService.publish(request.type(), request.version(), request.content());
        auditService.record(principal, "Terms", String.valueOf(terms.getId()), "PUBLISH", null, terms.getType() + " " + terms.getVersion());
        return DtoMapper.terms(terms);
    }
}
