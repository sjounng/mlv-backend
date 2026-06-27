package kr.maribel.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShopService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MailTemplateRepository mailTemplateRepository;
    private final CashChargeRepository cashChargeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CashService cashService;
    private final MailService mailService;
    private final MaribelProperties properties;

    public ShopService(CategoryRepository categoryRepository,
                       ProductRepository productRepository,
                       MailTemplateRepository mailTemplateRepository,
                       CashChargeRepository cashChargeRepository,
                       PurchaseOrderRepository purchaseOrderRepository,
                       CashService cashService,
                       MailService mailService,
                       MaribelProperties properties) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.mailTemplateRepository = mailTemplateRepository;
        this.cashChargeRepository = cashChargeRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.cashService = cashService;
        this.mailService = mailService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<Category> activeCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(Long categoryId, Boolean recommended, Boolean newBadge, Long minPrice, Long maxPrice, String keyword, boolean activeOnly) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : "";
        return productRepository.search(categoryId, activeOnly, recommended, newBadge, minPrice, maxPrice, normalizedKeyword);
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

    @Transactional
    public CashCharge createCashCharge(Member member, CashChargeRequest request) {
        String merchantOrderId = "cash_" + UUID.randomUUID().toString().replace("-", "");
        return cashChargeRepository.save(new CashCharge(merchantOrderId, member, request.cashAmount(), request.paymentAmountKrw()));
    }

    public String paymentUrl(CashCharge charge) {
        return properties.getStella().getPaymentBaseUrl()
                + "?merchantOrderId=" + charge.getMerchantOrderId()
                + "&amount=" + charge.getPaymentAmountKrw();
    }

    @Transactional
    public CashCharge handleStellaWebhook(StellaWebhookRequest request, String signatureHeader) {
        if (!isValidStellaSignature(request, signatureHeader)) {
            throw ApiException.unauthorized("INVALID_STELLA_SIGNATURE", "Stella IT 웹훅 서명이 올바르지 않습니다.");
        }

        CashCharge charge = cashChargeRepository.findByMerchantOrderId(request.merchantOrderId())
                .orElseThrow(() -> ApiException.notFound("CASH_CHARGE_NOT_FOUND", "충전 주문을 찾을 수 없습니다."));

        String status = request.status().trim().toUpperCase();
        if ("PAID".equals(status) || "SUCCESS".equals(status) || "SUCCEEDED".equals(status)) {
            if (charge.getStatus() == ChargeStatus.PAID) {
                return charge;
            }
            if (request.paidAmountKrw() != charge.getPaymentAmountKrw()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 주문 금액과 다릅니다.");
            }
            charge.markPaid(request.stellaPaymentId(), request.receiptUrl());
            cashService.credit(charge.getMember(), charge.getCashAmount(), CashTransactionType.CHARGE, charge.getMerchantOrderId(), "Stella IT cash charge");
            return charge;
        }

        if ("FAILED".equals(status) || "CANCELLED".equals(status) || "CANCELED".equals(status)) {
            charge.markFailed();
            return charge;
        }

        throw ApiException.badRequest("UNKNOWN_STELLA_STATUS", "알 수 없는 Stella IT 결제 상태입니다.");
    }

    @Transactional
    public PurchaseOrder purchase(Member member, PurchaseRequest request) {
        Product product = getProduct(request.productId(), true);
        int quantity = request.quantity();
        long totalPrice = product.getPrice() * quantity;
        String orderNumber = "po_" + UUID.randomUUID().toString().replace("-", "");

        product.decreaseStock(quantity);
        cashService.debit(member, totalPrice, orderNumber, "Product purchase: " + product.getName());

        PurchaseOrder order = purchaseOrderRepository.save(new PurchaseOrder(orderNumber, member, product, quantity, totalPrice));
        OutboundMail mail = mailService.enqueue(member, product.getMailTemplate(), MailSourceType.PURCHASE, orderNumber);
        order.attachMail(mail);
        return order;
    }

    @Transactional
    public Category upsertCategory(Long id, String name, int sortOrder, boolean active) {
        if (id == null) {
            return categoryRepository.save(new Category(name, sortOrder));
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."));
        category.update(name, sortOrder, active);
        return category;
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
                    request.active(), request.stockQuantity(), request.recommended(), request.newBadge());
            return productRepository.save(product);
        }

        Product product = productRepository.findWithCategoryAndMailTemplateById(id)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
        product.update(request.name(), request.description(), request.price(), request.imageUrl(), category, template,
                request.active(), request.stockQuantity(), request.recommended(), request.newBadge());
        return product;
    }

    private boolean isValidStellaSignature(StellaWebhookRequest request, String signatureHeader) {
        if (!StringUtils.hasText(signatureHeader)) {
            return properties.getStella().getWebhookSecret().startsWith("dev-");
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
