package kr.maribel.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import kr.maribel.backend.api.ApiDtos.CashChargeRequest;
import kr.maribel.backend.api.ApiDtos.ProductUpsertRequest;
import kr.maribel.backend.api.ApiDtos.PurchaseRequest;
import kr.maribel.backend.api.ApiDtos.StellaWebhookRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.Category;
import kr.maribel.backend.domain.DomainEnums;
import kr.maribel.backend.domain.DomainEnums.CashTransactionType;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import kr.maribel.backend.domain.DomainEnums.MailSourceType;
import kr.maribel.backend.domain.MailTemplate;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.OutboundMail;
import kr.maribel.backend.domain.Product;
import kr.maribel.backend.domain.PurchaseOrder;
import kr.maribel.backend.repository.CashChargeRepository;
import kr.maribel.backend.repository.CategoryRepository;
import kr.maribel.backend.repository.MailTemplateRepository;
import kr.maribel.backend.repository.ProductRepository;
import kr.maribel.backend.repository.PurchaseOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MailTemplateRepository mailTemplateRepository;
    private final CashChargeRepository cashChargeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CashService cashService;
    private final MailService mailService;
    private final MaribelProperties properties;
    private final SiteSettingService siteSettingService;

    public ShopService(CategoryRepository categoryRepository,
                       ProductRepository productRepository,
                       MailTemplateRepository mailTemplateRepository,
                       CashChargeRepository cashChargeRepository,
                       PurchaseOrderRepository purchaseOrderRepository,
                       CashService cashService,
                       MailService mailService,
                       MaribelProperties properties,
                       SiteSettingService siteSettingService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.mailTemplateRepository = mailTemplateRepository;
        this.cashChargeRepository = cashChargeRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.cashService = cashService;
        this.mailService = mailService;
        this.properties = properties;
        this.siteSettingService = siteSettingService;
    }

    @Transactional(readOnly = true)
    public List<Category> activeCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(Long categoryId, Boolean recommended, Boolean newBadge, Long minPrice, Long maxPrice, String keyword, boolean activeOnly, Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : "";
        return productRepository.search(categoryId, activeOnly, recommended, newBadge, minPrice, maxPrice, normalizedKeyword, pageable);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id, boolean activeOnly) {
        Product product = productRepository.findWithCategoryAndMailTemplateById(id)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
        if (activeOnly && !product.isActive()) {
            throw ApiException.notFound("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.");
        }
        return product;
    }

    // 충전 시 지급 캐시량은 서버가 결정한다. 클라이언트가 보낸 cashAmount 는 신뢰하지 않는다.
    //  (기본 환율 1원 = 1캐시. maribel.cash.krw-per-cash 로 조정) — 07-12 점검 H1
    private static final long CHARGE_MIN_KRW = 100L;
    private static final long CHARGE_MAX_KRW = 1_000_000L;

    @Transactional
    public CashCharge createCashCharge(Member member, CashChargeRequest request) {
        if (!siteSettingService.isShopEnabled()) {
            throw ApiException.badRequest("SHOP_DISABLED", "현재 상점이 비활성화 상태입니다.");
        }
        long paymentKrw = request.paymentAmountKrw();
        if (paymentKrw < CHARGE_MIN_KRW || paymentKrw > CHARGE_MAX_KRW) {
            throw ApiException.badRequest("INVALID_CHARGE_AMOUNT",
                    "충전 금액은 " + CHARGE_MIN_KRW + "원 ~ " + CHARGE_MAX_KRW + "원 사이여야 합니다.");
        }
        long krwPerCash = Math.max(1L, properties.getCash().getKrwPerCash());
        long cashAmount = paymentKrw / krwPerCash; // 서버 산출
        String merchantOrderId = "cash_" + UUID.randomUUID().toString().replace("-", "");
        return cashChargeRepository.save(new CashCharge(merchantOrderId, member, cashAmount, paymentKrw));
    }

    public String paymentUrl(CashCharge charge) {
        return properties.getStella().getPaymentBaseUrl()
                + "?merchantOrderId=" + charge.getMerchantOrderId()
                + "&amount=" + charge.getPaymentAmountKrw();
    }

    @Transactional
    public CashCharge handleStellaWebhook(StellaWebhookRequest request, String signatureHeader) {
        if (!isValidStellaSignature(request, signatureHeader)) {
            log.warn("stella webhook rejected: invalid signature, merchantOrderId={}", request.merchantOrderId());
            throw ApiException.unauthorized("INVALID_STELLA_SIGNATURE", "Stella IT 웹훅 서명이 올바르지 않습니다.");
        }

        // 동시/재전송 웹훅에 의한 이중 지급을 막기 위해 행을 잠근 뒤 상태 전이를 판정한다. (점검 H2/M3)
        CashCharge charge = cashChargeRepository.findByMerchantOrderIdForUpdate(request.merchantOrderId())
                .orElseThrow(() -> ApiException.notFound("CASH_CHARGE_NOT_FOUND", "충전 주문을 찾을 수 없습니다."));

        String status = request.status().trim().toUpperCase();
        if ("PAID".equals(status) || "SUCCESS".equals(status) || "SUCCEEDED".equals(status)) {
            // READY 상태에서만 지급한다. 이미 PAID/FAILED/REFUNDED 면 재지급/재전송 지급을 막는다.
            if (charge.getStatus() != ChargeStatus.READY) {
                log.info("stella webhook PAID ignored (status={}): merchantOrderId={}", charge.getStatus(), charge.getMerchantOrderId());
                return charge;
            }
            if (request.paidAmountKrw() != charge.getPaymentAmountKrw()) {
                log.warn("stella webhook amount mismatch: merchantOrderId={}, expected={}, paid={}",
                        charge.getMerchantOrderId(), charge.getPaymentAmountKrw(), request.paidAmountKrw());
                throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 주문 금액과 다릅니다.");
            }
            charge.markPaid(request.stellaPaymentId(), request.receiptUrl());
            cashService.credit(charge.getMember(), charge.getCashAmount(), CashTransactionType.CHARGE, charge.getMerchantOrderId(), "Stella IT cash charge");
            log.info("cash charge paid: merchantOrderId={}, memberId={}, cashAmount={}, paymentAmountKrw={}",
                    charge.getMerchantOrderId(), charge.getMember().getId(), charge.getCashAmount(), charge.getPaymentAmountKrw());
            return charge;
        }

        if ("FAILED".equals(status) || "CANCELLED".equals(status) || "CANCELED".equals(status)) {
            // 이미 지급 완료(PAID)/환불(REFUNDED) 건은 실패로 덮어쓰지 않는다. READY 만 실패 처리.
            if (charge.getStatus() != ChargeStatus.READY) {
                log.info("stella webhook FAILED ignored (status={}): merchantOrderId={}", charge.getStatus(), charge.getMerchantOrderId());
                return charge;
            }
            charge.markFailed();
            log.info("cash charge failed: merchantOrderId={}, status={}", charge.getMerchantOrderId(), status);
            return charge;
        }

        throw ApiException.badRequest("UNKNOWN_STELLA_STATUS", "알 수 없는 Stella IT 결제 상태입니다.");
    }

    @Transactional
    public PurchaseOrder purchase(Member member, PurchaseRequest request) {
        if (!siteSettingService.isShopEnabled()) {
            throw ApiException.badRequest("SHOP_DISABLED", "현재 상점이 비활성화 상태입니다.");
        }
        // 동시 구매 시 재고 초과 판매를 막기 위해 상품 행을 잠근 뒤 차감한다.
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
        if (!product.isActive()) {
            throw ApiException.notFound("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.");
        }
        int quantity = request.quantity();
        assertPurchaseLimit(member, product, quantity);
        long totalPrice = product.getPrice() * quantity;
        String orderNumber = "po_" + UUID.randomUUID().toString().replace("-", "");

        try {
            product.decreaseStock(quantity);
        } catch (IllegalStateException exception) {
            throw ApiException.badRequest("INSUFFICIENT_STOCK", "상품 재고가 부족합니다.");
        }
        cashService.debit(member, totalPrice, orderNumber, "Product purchase: " + product.getName());

        PurchaseOrder order = purchaseOrderRepository.save(new PurchaseOrder(orderNumber, member, product, quantity, totalPrice));
        OutboundMail mail = mailService.enqueue(member, product.getMailTemplate(), MailSourceType.PURCHASE, orderNumber);
        order.attachMail(mail);
        log.info("purchase completed: orderNumber={}, memberId={}, productId={}, quantity={}, totalPrice={}",
                orderNumber, member.getId(), product.getId(), quantity, totalPrice);
        return order;
    }

    // 구매 제한 검증 (07-12 피드백): 리셋 경계는 KST 오전 6시 기준.
    //  WEEKLY = 직전 월요일 06:00, MONTHLY = 이번 달 1일 06:00, ONCE = 전체 기간.
    private void assertPurchaseLimit(Member member, Product product, int quantity) {
        DomainEnums.PurchaseLimitType type = product.getPurchaseLimitType();
        if (type == null || type == DomainEnums.PurchaseLimitType.NONE) {
            return;
        }
        Instant since = switch (type) {
            case WEEKLY -> lastResetWeekly();
            case MONTHLY -> lastResetMonthly();
            default -> Instant.EPOCH; // ONCE
        };
        int limit = type == DomainEnums.PurchaseLimitType.ONCE ? 1 : Math.max(1, product.getPurchaseLimitCount());
        long already = purchaseOrderRepository.sumQuantitySince(member.getId(), product.getId(), since);
        if (already + quantity > limit) {
            String label = switch (type) {
                case WEEKLY -> "매주 월요일 오전 6시에 초기화되는 상품입니다.";
                case MONTHLY -> "매월 1일 오전 6시에 초기화되는 상품입니다.";
                default -> "계정 당 " + limit + "회만 구매할 수 있는 상품입니다.";
            };
            throw ApiException.badRequest("PURCHASE_LIMIT_EXCEEDED",
                    "구매 가능 횟수를 초과했습니다. (" + already + "/" + limit + ") " + label);
        }
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private Instant lastResetWeekly() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime candidate = now.with(java.time.DayOfWeek.MONDAY).withHour(6).withMinute(0).withSecond(0).withNano(0);
        if (candidate.isAfter(now)) {
            candidate = candidate.minusWeeks(1);
        }
        return candidate.toInstant();
    }

    private Instant lastResetMonthly() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime candidate = now.withDayOfMonth(1).withHour(6).withMinute(0).withSecond(0).withNano(0);
        if (candidate.isAfter(now)) {
            candidate = candidate.minusMonths(1);
        }
        return candidate.toInstant();
    }

    @Transactional
    public Category upsertCategory(Long id, String name, int sortOrder, boolean active) {
        if (id == null) {
            return categoryRepository.save(new Category(name, sortOrder, active));
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."));
        category.update(name, sortOrder, active);
        return category;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."));
        // 카테고리에 상품이 있으면 삭제 불가 (FK 보호). 상품을 옮기거나 삭제 후 처리.
        if (productRepository.countByCategoryId(id) > 0) {
            throw ApiException.badRequest("CATEGORY_HAS_PRODUCTS", "카테고리에 상품이 있어 삭제할 수 없습니다. 상품을 옮기거나 삭제해 주세요.");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public MailTemplate createMailTemplate(String mailCode, String subject, String content, String rewardsJson) {
        if (mailTemplateRepository.findByMailCode(mailCode).isPresent()) {
            throw ApiException.conflict("MAIL_TEMPLATE_DUPLICATED", "이미 존재하는 우편 코드입니다.");
        }
        return mailTemplateRepository.save(new MailTemplate(mailCode, subject, content, rewardsJson));
    }

    @Transactional
    public Product upsertProduct(Long id, ProductUpsertRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."));
        MailTemplate template = mailTemplateRepository.findById(request.mailTemplateId())
                .orElseThrow(() -> ApiException.notFound("MAIL_TEMPLATE_NOT_FOUND", "우편 템플릿을 찾을 수 없습니다."));

        if (id == null) {
            Product product = new Product(request.name(), request.description(), request.price(), request.imageUrl(), category, template);
            product.update(request.name(), request.description(), request.price(), request.imageUrl(), category, template,
                    request.active(), request.stockQuantity(), request.recommended(), request.newBadge(),
                    request.purchaseLimitType(), request.purchaseLimitCount() == null ? 1 : request.purchaseLimitCount());
            return productRepository.save(product);
        }

        Product product = productRepository.findWithCategoryAndMailTemplateById(id)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
        product.update(request.name(), request.description(), request.price(), request.imageUrl(), category, template,
                request.active(), request.stockQuantity(), request.recommended(), request.newBadge(),
                request.purchaseLimitType(), request.purchaseLimitCount() == null ? 1 : request.purchaseLimitCount());
        return product;
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
        // 구매 이력이 있으면 삭제 불가 (이력 보존/FK 보호). 판매 중지(비공개)로 전환 권장.
        if (purchaseOrderRepository.countByProductId(id) > 0) {
            throw ApiException.badRequest("PRODUCT_HAS_ORDERS", "구매 이력이 있어 삭제할 수 없습니다. 판매 중지(비공개)로 전환해 주세요.");
        }
        productRepository.delete(product);
    }

    private boolean isValidStellaSignature(StellaWebhookRequest request, String signatureHeader) {
        if (!StringUtils.hasText(signatureHeader)) {
            return properties.getStella().isAllowUnsignedWebhook();
        }
        String payload = request.merchantOrderId() + ":" + request.stellaPaymentId() + ":" + request.status() + ":" + request.paidAmountKrw();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getStella().getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // 헤더의 Base64 시그니처를 디코딩한 원본 바이트끼리 constant-time 비교한다.
            byte[] provided = Base64.getDecoder().decode(signatureHeader.trim());
            return MessageDigest.isEqual(expected, provided);
        } catch (IllegalArgumentException invalidBase64) {
            return false;
        } catch (Exception exception) {
            throw new IllegalStateException("failed to verify stella signature", exception);
        }
    }
}
