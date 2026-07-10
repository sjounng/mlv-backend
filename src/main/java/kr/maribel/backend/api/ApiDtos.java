package kr.maribel.backend.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import kr.maribel.backend.domain.DomainEnums.BannerPlacement;
import kr.maribel.backend.domain.DomainEnums.CashTransactionType;
import kr.maribel.backend.domain.DomainEnums.ChargeStatus;
import kr.maribel.backend.domain.DomainEnums.ContactCategory;
import kr.maribel.backend.domain.DomainEnums.ContactStatus;
import kr.maribel.backend.domain.DomainEnums.EventType;
import kr.maribel.backend.domain.DomainEnums.MailSourceType;
import kr.maribel.backend.domain.DomainEnums.OutboundMailStatus;
import kr.maribel.backend.domain.DomainEnums.PurchaseStatus;
import kr.maribel.backend.domain.DomainEnums.RefundStatus;
import kr.maribel.backend.domain.DomainEnums.Role;
import kr.maribel.backend.domain.DomainEnums.ServerOpenStatus;
import kr.maribel.backend.domain.DomainEnums.TermsType;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import org.springframework.data.domain.Page;

public final class ApiDtos {

    private ApiDtos() {
    }

    /** 페이지네이션 응답 공통 래퍼. */
    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        public static <T> PageResponse<T> of(Page<?> source, List<T> content) {
            return new PageResponse<>(
                    content,
                    source.getNumber(),
                    source.getSize(),
                    source.getTotalElements(),
                    source.getTotalPages(),
                    source.hasNext()
            );
        }
    }

    public record DevLoginRequest(
            @NotBlank String microsoftSub,
            @NotBlank @Size(max = 36) String minecraftUuid,
            @NotBlank @Size(max = 32) String minecraftUsername,
            @Email String email,
            boolean marketingAgreed
    ) {
    }

    public record AdminLoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            long refreshExpiresInSeconds
    ) {
    }

    public record MicrosoftAuthorizeUrlResponse(
            String authorizationUrl,
            boolean configured
    ) {
    }

    public record ProfileResponse(
            Long id,
            String minecraftUuid,
            String minecraftUsername,
            String email,
            UserStatus status,
            Instant agreedTermsAt,
            int warningCount,
            Instant createdAt
    ) {
    }

    public record ServerStatusResponse(
            ServerOpenStatus status,
            String message,
            int onlinePlayers
    ) {
    }

    public record TermsResponse(
            Long id,
            TermsType type,
            String version,
            String content,
            Instant publishedAt
    ) {
    }

    public record AttendanceResponse(
            Long eventId,
            String eventName,
            String today,
            boolean todayClaimed,
            List<String> claimedDates,
            boolean claimable
    ) {
    }

    public record TermsCreateRequest(
            @NotNull TermsType type,
            @NotBlank @Size(max = 40) String version,
            @NotBlank String content
    ) {
    }

    public record PopupRequest(
            @NotBlank @Size(max = 500) String imageUrl,
            @Size(max = 500) String linkUrl,
            // 노출 위치(HOME/EVENT). 미지정 시 서비스에서 EVENT 로 처리(구버전 호환)
            BannerPlacement placement,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            boolean active
    ) {
    }

    public record UploadResponse(String url) {
    }

    public record NoticeRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank String content,
            boolean pinned
    ) {
    }

    public record NoticeResponse(
            Long id,
            String title,
            String content,
            boolean pinned,
            Instant createdAt
    ) {
    }

    public record PopupResponse(
            Long id,
            String imageUrl,
            String linkUrl,
            BannerPlacement placement,
            Instant startAt,
            Instant endAt,
            boolean active,
            Instant createdAt
    ) {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 80) String name,
            int sortOrder,
            boolean active
    ) {
    }

    public record CategoryResponse(
            Long id,
            String name,
            int sortOrder,
            boolean active
    ) {
    }

    public record MailTemplateRequest(
            @NotBlank @Size(max = 80) String mailCode,
            @NotBlank @Size(max = 160) String subject,
            @NotBlank String content,
            @NotBlank String rewardsJson
    ) {
    }

    public record MailTemplateResponse(
            Long id,
            String mailCode,
            String subject,
            String content,
            String rewardsJson,
            Instant createdAt
    ) {
    }

    public record ProductUpsertRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank String description,
            @Positive long price,
            String imageUrl,
            @NotNull Long categoryId,
            @NotNull Long mailTemplateId,
            boolean active,
            @PositiveOrZero Integer stockQuantity,
            boolean recommended,
            boolean newBadge
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            String description,
            long price,
            String imageUrl,
            CategoryResponse category,
            Long mailTemplateId,
            boolean active,
            Integer stockQuantity,
            boolean recommended,
            boolean newBadge
    ) {
    }

    public record CashChargeRequest(
            @Positive long cashAmount,
            @Positive long paymentAmountKrw
    ) {
    }

    public record CashChargeResponse(
            Long id,
            String merchantOrderId,
            long cashAmount,
            long paymentAmountKrw,
            ChargeStatus status,
            String paymentUrl,
            String receiptUrl,
            Instant createdAt
    ) {
    }

    public record StellaWebhookRequest(
            @NotBlank String merchantOrderId,
            @NotBlank String stellaPaymentId,
            @NotBlank String status,
            @PositiveOrZero long paidAmountKrw,
            String receiptUrl
    ) {
    }

    public record PurchaseRequest(
            @NotNull Long productId,
            @Min(1) int quantity
    ) {
    }

    public record PurchaseResponse(
            Long id,
            String orderNumber,
            long totalPrice,
            PurchaseStatus status,
            MailResponse outboundMail
    ) {
    }

    public record CashBalanceResponse(
            long balance,
            List<CashTransactionResponse> recentTransactions
    ) {
    }

    public record CashTransactionResponse(
            Long id,
            CashTransactionType type,
            long amount,
            long balanceAfter,
            String refId,
            String memo,
            Instant createdAt
    ) {
    }

    public record ChargeHistoryResponse(
            Long id,
            String merchantOrderId,
            long cashAmount,
            long paymentAmountKrw,
            ChargeStatus status,
            String stellaPaymentId,
            String receiptUrl,
            Instant createdAt,
            Instant paidAt
    ) {
    }

    public record PurchaseHistoryResponse(
            Long id,
            String orderNumber,
            String productName,
            int quantity,
            long totalPrice,
            PurchaseStatus status,
            MailResponse outboundMail,
            Instant createdAt
    ) {
    }

    public record RefundCreateRequest(
            @NotNull Long cashChargeId,
            @NotBlank String reason
    ) {
    }

    public record RefundProcessRequest(
            @NotNull RefundStatus status,
            String operatorMemo
    ) {
    }

    public record RefundResponse(
            Long id,
            Long cashChargeId,
            String reason,
            RefundStatus status,
            String operatorMemo,
            Instant processedAt,
            Instant createdAt
    ) {
    }

    public record EventUpsertRequest(
            @NotBlank String name,
            @NotNull EventType type,
            @NotBlank String description,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @NotNull Long mailTemplateId,
            boolean active
    ) {
    }

    public record EventResponse(
            Long id,
            String name,
            EventType type,
            String description,
            Instant startAt,
            Instant endAt,
            boolean active,
            Long mailTemplateId
    ) {
    }

    public record ClaimResponse(
            Long participationId,
            MailResponse outboundMail
    ) {
    }

    public record AgreeTermsRequest(
            boolean marketingAgreed
    ) {
    }

    public record RedeemUseRequest(@NotBlank String code) {
    }

    public record RedeemCodeCreateRequest(
            @NotBlank String code,
            @NotNull Long mailTemplateId,
            @Positive int maxUses,
            @Future Instant expiresAt
    ) {
    }

    public record RedeemCodeResponse(
            Long id,
            String code,
            int maxUses,
            int usedCount,
            Instant expiresAt,
            boolean active
    ) {
    }

    public record MailResponse(
            Long id,
            String targetUuid,
            String mailCode,
            String subject,
            String content,
            String rewardsJson,
            MailSourceType sourceType,
            String sourceRefId,
            OutboundMailStatus status,
            int retryCount,
            Instant nextRetryAt,
            Instant sentAt,
            String lastError,
            Instant createdAt
    ) {
    }

    public record AdminMailSendRequest(
            @NotBlank @Size(max = 36) String targetUuid,
            @NotNull Long mailTemplateId,
            @NotBlank String sourceRefId
    ) {
    }

    public record WebpanelAckRequest(
            @NotNull OutboundMailStatus status,
            String errorMessage,
            boolean retryable
    ) {
    }

    public record InquiryCreateRequest(
            @NotNull ContactCategory category,
            @NotBlank @Size(max = 160) String title,
            @NotBlank String content,
            String attachmentUrl
    ) {
    }

    public record InquiryReplyRequest(@NotBlank String content) {
    }

    public record InquiryResponse(
            Long id,
            ContactCategory category,
            String title,
            String content,
            String attachmentUrl,
            ContactStatus status,
            Instant createdAt
    ) {
    }

    public record DashboardResponse(
            long activeUsers,
            long paidCharges,
            long pendingMails,
            long failedMails,
            long pendingRefunds,
            long openInquiries
    ) {
    }

    public record AdminMemberResponse(
            Long id,
            String microsoftSub,
            String minecraftUuid,
            String minecraftUsername,
            String email,
            UserStatus status,
            Instant createdAt
    ) {
    }

    public record AdminAccountResponse(
            Long id,
            String username,
            Role role,
            Instant lastLoginAt,
            boolean active
    ) {
    }

    public record AuditLogResponse(
            Long id,
            String actor,
            String entityType,
            String entityId,
            String action,
            String oldValue,
            String newValue,
            Instant createdAt
    ) {
    }
}
