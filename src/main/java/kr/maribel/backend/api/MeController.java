package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.CashBalanceResponse;
import kr.maribel.backend.api.ApiDtos.ChargeHistoryResponse;
import kr.maribel.backend.api.ApiDtos.MailResponse;
import kr.maribel.backend.api.ApiDtos.ProfileResponse;
import kr.maribel.backend.api.ApiDtos.PurchaseHistoryResponse;
import kr.maribel.backend.api.ApiDtos.RefundCreateRequest;
import kr.maribel.backend.api.ApiDtos.RefundResponse;
import kr.maribel.backend.domain.CashBalance;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.repository.CashChargeRepository;
import kr.maribel.backend.repository.CashTransactionRepository;
import kr.maribel.backend.repository.PurchaseOrderRepository;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.service.CashService;
import kr.maribel.backend.service.MailService;
import kr.maribel.backend.service.MemberService;
import kr.maribel.backend.service.RefundService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Me")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MeController {

    private final MemberService memberService;
    private final CashService cashService;
    private final MailService mailService;
    private final RefundService refundService;
    private final CashTransactionRepository cashTransactionRepository;
    private final CashChargeRepository cashChargeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public MeController(MemberService memberService,
                        CashService cashService,
                        MailService mailService,
                        RefundService refundService,
                        CashTransactionRepository cashTransactionRepository,
                        CashChargeRepository cashChargeRepository,
                        PurchaseOrderRepository purchaseOrderRepository) {
        this.memberService = memberService;
        this.cashService = cashService;
        this.mailService = mailService;
        this.refundService = refundService;
        this.cashTransactionRepository = cashTransactionRepository;
        this.cashChargeRepository = cashChargeRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @GetMapping("/profile")
    @Operation(summary = "내 프로필 조회")
    ProfileResponse profile(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return DtoMapper.profile(memberService.getActiveMember(principal.memberId()));
    }

    @GetMapping("/cash")
    @Operation(summary = "내 캐시 잔액과 최근 변동 내역 조회")
    CashBalanceResponse cash(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Member member = memberService.getActiveMember(principal.memberId());
        CashBalance balance = cashService.getBalance(member);
        return new CashBalanceResponse(
                balance.getBalance(),
                cashTransactionRepository.findTop50ByMemberIdOrderByCreatedAtDesc(member.getId()).stream()
                        .map(DtoMapper::cashTransaction)
                        .toList()
        );
    }

    @GetMapping("/charges")
    @Operation(summary = "내 캐시 충전 내역 조회")
    List<ChargeHistoryResponse> charges(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Member member = memberService.getActiveMember(principal.memberId());
        return cashChargeRepository.findTop50ByMemberIdOrderByCreatedAtDesc(member.getId()).stream()
                .map(DtoMapper::chargeHistory)
                .toList();
    }

    @GetMapping("/purchases")
    @Operation(summary = "내 상품 구매 내역 조회")
    List<PurchaseHistoryResponse> purchases(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Member member = memberService.getActiveMember(principal.memberId());
        return purchaseOrderRepository.findTop50ByMemberIdOrderByCreatedAtDesc(member.getId()).stream()
                .map(DtoMapper::purchaseHistory)
                .toList();
    }

    @GetMapping("/mails")
    @Operation(summary = "내 인게임 우편 발송 이력 조회")
    List<MailResponse> mails(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Member member = memberService.getActiveMember(principal.memberId());
        return mailService.listForMember(member).stream().map(DtoMapper::mail).toList();
    }

    @PostMapping("/refunds")
    @Operation(summary = "환불 요청 생성")
    RefundResponse requestRefund(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                 @Valid @RequestBody RefundCreateRequest request) {
        Member member = memberService.getActiveMember(principal.memberId());
        return DtoMapper.refund(refundService.create(member, request));
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴")
    void withdraw(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        memberService.withdraw(principal.memberId());
    }
}
