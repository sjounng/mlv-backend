package kr.maribel.backend.api;

import kr.maribel.backend.api.ApiDtos.AdminAccountResponse;
import kr.maribel.backend.api.ApiDtos.AdminMemberResponse;
import kr.maribel.backend.api.ApiDtos.AuditLogResponse;
import kr.maribel.backend.api.ApiDtos.CashChargeResponse;
import kr.maribel.backend.api.ApiDtos.CashProductResponse;
import kr.maribel.backend.api.ApiDtos.CashTransactionResponse;
import kr.maribel.backend.api.ApiDtos.CategoryResponse;
import kr.maribel.backend.api.ApiDtos.ChargeHistoryResponse;
import kr.maribel.backend.api.ApiDtos.EventResponse;
import kr.maribel.backend.api.ApiDtos.InquiryResponse;
import kr.maribel.backend.api.ApiDtos.MailResponse;
import kr.maribel.backend.api.ApiDtos.MailTemplateResponse;
import kr.maribel.backend.api.ApiDtos.NoticeResponse;
import kr.maribel.backend.api.ApiDtos.PopupResponse;
import kr.maribel.backend.api.ApiDtos.ProductResponse;
import kr.maribel.backend.api.ApiDtos.ProfileResponse;
import kr.maribel.backend.api.ApiDtos.PurchaseHistoryResponse;
import kr.maribel.backend.api.ApiDtos.PurchaseResponse;
import kr.maribel.backend.api.ApiDtos.RedeemCodeResponse;
import kr.maribel.backend.api.ApiDtos.RefundResponse;
import kr.maribel.backend.api.ApiDtos.TermsResponse;
import kr.maribel.backend.api.ApiDtos.WarningResponse;
import kr.maribel.backend.domain.AdminAccount;
import kr.maribel.backend.domain.AuditLog;
import kr.maribel.backend.domain.CashCharge;
import kr.maribel.backend.domain.CashTransaction;
import kr.maribel.backend.domain.Category;
import kr.maribel.backend.domain.ContactInquiry;
import kr.maribel.backend.domain.MailTemplate;
import kr.maribel.backend.domain.MaribelEvent;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.domain.Notice;
import kr.maribel.backend.domain.Warning;
import kr.maribel.backend.domain.OutboundMail;
import kr.maribel.backend.domain.Popup;
import kr.maribel.backend.domain.Product;
import kr.maribel.backend.domain.PurchaseOrder;
import kr.maribel.backend.domain.RedeemCode;
import kr.maribel.backend.domain.RefundRequest;
import kr.maribel.backend.domain.TermsDocument;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static ProfileResponse profile(Member member) {
        return new ProfileResponse(
                member.getId(),
                member.getMinecraftUuid(),
                member.getMinecraftUsername(),
                member.getEmail(),
                member.getStatus(),
                member.getRole(),
                member.getAgreedTermsAt(),
                member.getWarningCount(),
                member.getCreatedAt()
        );
    }

    public static CategoryResponse category(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSortOrder(), category.isActive());
    }

    public static MailTemplateResponse mailTemplate(MailTemplate template) {
        return new MailTemplateResponse(
                template.getId(),
                template.getMailCode(),
                template.getSubject(),
                template.getContent(),
                template.getRewardsJson(),
                template.getCreatedAt()
        );
    }

    public static ProductResponse product(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                category(product.getCategory()),
                product.getMailTemplate().getId(),
                product.isActive(),
                product.getStockQuantity(),
                product.isRecommended(),
                product.isNewBadge(),
                product.getPurchaseLimitType(),
                product.getPurchaseLimitCount()
        );
    }

    public static CashTransactionResponse cashTransaction(CashTransaction transaction) {
        return new CashTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getRefId(),
                transaction.getMemo(),
                transaction.getCreatedAt()
        );
    }

    public static CashChargeResponse cashCharge(CashCharge charge, String paymentUrl) {
        return new CashChargeResponse(
                charge.getId(),
                charge.getMerchantOrderId(),
                charge.getCashAmount(),
                charge.getPaymentAmountKrw(),
                charge.getStatus(),
                paymentUrl,
                charge.getReceiptUrl(),
                charge.getCreatedAt()
        );
    }

    public static ChargeHistoryResponse chargeHistory(CashCharge charge) {
        return new ChargeHistoryResponse(
                charge.getId(),
                charge.getMerchantOrderId(),
                charge.getCashAmount(),
                charge.getPaymentAmountKrw(),
                charge.getStatus(),
                charge.getStellaPaymentId(),
                charge.getReceiptUrl(),
                charge.getCreatedAt(),
                charge.getPaidAt()
        );
    }

    public static MailResponse mail(OutboundMail mail) {
        return new MailResponse(
                mail.getId(),
                mail.getTargetUuid(),
                mail.getMailCode(),
                mail.getSubject(),
                mail.getContent(),
                mail.getRewardsJson(),
                mail.getSourceType(),
                mail.getSourceRefId(),
                mail.getStatus(),
                mail.getRetryCount(),
                mail.getNextRetryAt(),
                mail.getSentAt(),
                mail.getLastError(),
                mail.getCreatedAt()
        );
    }

    public static PurchaseResponse purchase(PurchaseOrder order) {
        return new PurchaseResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getOutboundMail() == null ? null : mail(order.getOutboundMail())
        );
    }

    public static PurchaseHistoryResponse purchaseHistory(PurchaseOrder order) {
        return new PurchaseHistoryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getProduct().getName(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getOutboundMail() == null ? null : mail(order.getOutboundMail()),
                order.getCreatedAt()
        );
    }

    public static RefundResponse refund(RefundRequest refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getCashCharge().getId(),
                refund.getReason(),
                refund.getStatus(),
                refund.getOperatorMemo(),
                refund.getProcessedAt(),
                refund.getCreatedAt()
        );
    }

    public static CashProductResponse cashProduct(kr.maribel.backend.domain.CashProduct p) {
        return new CashProductResponse(
                p.getId(),
                p.getName(),
                p.getPriceKrw(),
                p.getCashAmount(),
                p.getIconUrl(),
                p.getSortOrder(),
                p.isActive()
        );
    }

    public static EventResponse event(MaribelEvent event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getType(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.isActive(),
                event.getMailTemplate().getId()
        );
    }

    public static RedeemCodeResponse redeemCode(RedeemCode code) {
        return new RedeemCodeResponse(
                code.getId(),
                code.getCode(),
                code.getMaxUses(),
                code.getUsedCount(),
                code.getExpiresAt(),
                code.isActive()
        );
    }

    public static InquiryResponse inquiry(ContactInquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getAttachmentUrl(),
                inquiry.getStatus(),
                inquiry.getCreatedAt()
        );
    }

    public static TermsResponse terms(TermsDocument terms) {
        return new TermsResponse(
                terms.getId(),
                terms.getType(),
                terms.getVersion(),
                terms.getContent(),
                terms.getPublishedAt()
        );
    }

    public static NoticeResponse notice(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.getCreatedAt()
        );
    }

    public static PopupResponse popup(Popup popup) {
        return new PopupResponse(
                popup.getId(),
                popup.getImageUrl(),
                popup.getLinkUrl(),
                popup.getPlacement(),
                popup.getStartAt(),
                popup.getEndAt(),
                popup.isActive(),
                popup.getCreatedAt()
        );
    }

    public static WarningResponse warning(Warning w) {
        return new WarningResponse(
                w.getId(),
                w.getReason(),
                w.getDetail(),
                w.getCustomText(),
                w.getCountAtIssue(),
                w.getIssuedBy(),
                w.isCanceled(),
                w.getCanceledReason(),
                w.getCanceledAt(),
                w.getCreatedAt()
        );
    }

    public static AdminMemberResponse adminMember(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getMicrosoftSub(),
                member.getMinecraftUuid(),
                member.getMinecraftUsername(),
                member.getEmail(),
                member.getStatus(),
                member.getWarningCount(),
                member.getCreatedAt()
        );
    }

    public static AdminAccountResponse adminAccount(AdminAccount account) {
        return new AdminAccountResponse(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getLastLoginAt(),
                account.isActive()
        );
    }

    public static AuditLogResponse auditLog(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getOldValue(),
                log.getNewValue(),
                log.getCreatedAt()
        );
    }
}
