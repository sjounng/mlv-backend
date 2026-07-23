package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.CashChargeRequest;
import kr.maribel.backend.api.ApiDtos.CashChargeResponse;
import kr.maribel.backend.api.ApiDtos.CashProductDescriptionResponse;
import kr.maribel.backend.api.ApiDtos.CashProductResponse;
import kr.maribel.backend.api.ApiDtos.CategoryResponse;
import kr.maribel.backend.api.ApiDtos.ProductResponse;
import kr.maribel.backend.api.ApiDtos.PurchaseRequest;
import kr.maribel.backend.api.ApiDtos.PurchaseResponse;
import kr.maribel.backend.api.ApiDtos.StellaWebhookRequest;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.PurchaseOrder;
import kr.maribel.backend.security.AuthenticatedPrincipal;
import kr.maribel.backend.service.CashProductService;
import kr.maribel.backend.service.MemberService;
import kr.maribel.backend.service.ShopService;
import kr.maribel.backend.service.SiteSettingService;
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
    private final CashProductService cashProductService;
    private final SiteSettingService siteSettingService;

    public ShopController(ShopService shopService, MemberService memberService,
                          CashProductService cashProductService, SiteSettingService siteSettingService) {
        this.shopService = shopService;
        this.memberService = memberService;
        this.cashProductService = cashProductService;
        this.siteSettingService = siteSettingService;
    }

    @GetMapping("/api/shop/cash-products")
    @Operation(summary = "캐시 충전 상품 목록 조회")
    List<CashProductResponse> cashProducts() {
        return cashProductService.activeProducts().stream().map(DtoMapper::cashProduct).toList();
    }

    @GetMapping("/api/shop/cash-products/{id}")
    @Operation(summary = "캐시 충전 상품 상세 조회")
    CashProductResponse cashProduct(@PathVariable Long id) {
        return DtoMapper.cashProduct(cashProductService.activeProduct(id));
    }

    @GetMapping("/api/shop/cash-product-description")
    @Operation(summary = "캐시 충전 상품 공통 상세 소개")
    CashProductDescriptionResponse cashProductDescription() {
        return new CashProductDescriptionResponse(siteSettingService.getCashProductDescription());
    }

    @GetMapping("/api/shop/categories")
    @Operation(summary = "상점 카테고리 목록 조회")
    List<CategoryResponse> categories() {
        return shopService.activeCategories().stream().map(DtoMapper::category).toList();
    }

    @GetMapping("/api/shop/products")
    @Operation(summary = "상품 목록 검색 (page/size 지정 시 페이지네이션 응답)")
    Object products(@RequestParam(required = false) Long categoryId,
                    @RequestParam(required = false) Boolean recommended,
                    @RequestParam(required = false) Boolean newBadge,
                    @RequestParam(required = false) Long minPrice,
                    @RequestParam(required = false) Long maxPrice,
                    @RequestParam(required = false) String keyword,
                    @RequestParam(required = false) Integer page,
                    @RequestParam(required = false) Integer size) {
        // page/size 미지정 시 배열을 반환한다 (배포 중인 구버전 프론트 호환. 프론트 전환 후 제거 예정).
        boolean paged = page != null || size != null;
        Pageable pageable = paged
                ? PageRequest.of(Math.max(page == null ? 0 : page, 0), Math.clamp(size == null ? 24 : size, 1, 100))
                : Pageable.unpaged();
        Page<ProductResponse> result = shopService
                .searchProducts(categoryId, recommended, newBadge, minPrice, maxPrice, keyword, true, pageable)
                .map(DtoMapper::product);
        return paged ? ApiDtos.PageResponse.of(result, result.getContent()) : result.getContent();
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
