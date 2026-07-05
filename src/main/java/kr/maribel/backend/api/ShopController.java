package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.CashChargeRequest;
import kr.maribel.backend.api.ApiDtos.CashChargeResponse;
import kr.maribel.backend.api.ApiDtos.CategoryResponse;
import kr.maribel.backend.api.ApiDtos.ProductResponse;
import kr.maribel.backend.api.ApiDtos.PurchaseRequest;
import kr.maribel.backend.api.ApiDtos.PurchaseResponse;
import kr.maribel.backend.api.ApiDtos.StellaWebhookRequest;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.PurchaseOrder;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.service.MemberService;
import kr.maribel.backend.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Shop")
public class ShopController {

    private final ShopService shopService;
    private final MemberService memberService;

    public ShopController(ShopService shopService, MemberService memberService) {
        this.shopService = shopService;
        this.memberService = memberService;
    }

    @GetMapping("/api/shop/categories")
    @Operation(summary = "상점 카테고리 목록 조회")
    List<CategoryResponse> categories() {
        return shopService.activeCategories().stream().map(DtoMapper::category).toList();
    }

    @GetMapping("/api/shop/products")
    @Operation(summary = "상품 목록 검색 (페이지네이션)")
    ApiDtos.PageResponse<ProductResponse> products(@RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Boolean recommended,
                                                   @RequestParam(required = false) Boolean newBadge,
                                                   @RequestParam(required = false) Long minPrice,
                                                   @RequestParam(required = false) Long maxPrice,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        Page<ProductResponse> result = shopService
                .searchProducts(categoryId, recommended, newBadge, minPrice, maxPrice, keyword, true, pageable)
                .map(DtoMapper::product);
        return ApiDtos.PageResponse.of(result, result.getContent());
    }

    @GetMapping("/api/shop/products/{id}")
    @Operation(summary = "상품 상세 조회")
    ProductResponse product(@PathVariable Long id) {
        return DtoMapper.product(shopService.getProduct(id, true));
    }

    @PostMapping("/api/shop/cash/charges")
    @Operation(summary = "캐시 충전 주문 생성", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    CashChargeResponse createCashCharge(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                        @Valid @RequestBody CashChargeRequest request) {
        Member member = memberService.getActiveMember(principal.memberId());
        CashCharge charge = shopService.createCashCharge(member, request);
        return DtoMapper.cashCharge(charge, shopService.paymentUrl(charge));
    }

    @PostMapping("/api/shop/purchases")
    @Operation(summary = "보유 캐시로 상품 구매", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    PurchaseResponse purchase(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                              @Valid @RequestBody PurchaseRequest request) {
        Member member = memberService.getActiveMember(principal.memberId());
        PurchaseOrder order = shopService.purchase(member, request);
        return DtoMapper.purchase(order);
    }

    @PostMapping("/api/payments/stella/webhook")
    @Operation(summary = "Stella IT 결제 웹훅 처리", security = @SecurityRequirement(name = OpenApiConfig.STELLA_SIGNATURE))
    CashChargeResponse stellaWebhook(@RequestHeader(value = "X-Stella-Signature", required = false) String signature,
                                     @Valid @RequestBody StellaWebhookRequest request) {
        CashCharge charge = shopService.handleStellaWebhook(request, signature);
        return DtoMapper.cashCharge(charge, null);
    }
}
