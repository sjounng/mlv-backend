package kr.maribel.backend.domain;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum Role {
        USER,
        OPERATOR,
        SUPER_ADMIN
    }

    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        WITHDRAWN
    }

    public enum RefreshTokenOwnerType {
        MEMBER,
        ADMIN
    }

    public enum CashTransactionType {
        CHARGE,
        SPEND,
        REFUND,
        ADJUSTMENT
    }

    public enum ChargeStatus {
        READY,
        PAID,
        FAILED,
        CANCELLED,
        REFUNDED
    }

    public enum PurchaseStatus {
        COMPLETED,
        CANCELLED,
        MAIL_PENDING,
        MAIL_FAILED
    }

    public enum RefundStatus {
        REQUESTED,
        APPROVED,
        REJECTED,
        COMPLETED
    }

    public enum EventType {
        ATTENDANCE,
        INVITE,
        PAYBACK,
        GENERAL
    }

    public enum MailSourceType {
        EVENT,
        PURCHASE,
        ADMIN,
        REDEEM_CODE
    }

    public enum OutboundMailStatus {
        PENDING,
        SENT,
        FAILED,
        CANCELLED
    }

    public enum RedeemCodeStatus {
        ACTIVE,
        EXPIRED,
        DISABLED,
        EXHAUSTED
    }

    public enum ContactCategory {
        PAYMENT,
        ACCOUNT,
        EVENT,
        OTHER
    }

    public enum ContactStatus {
        OPEN,
        ANSWERED,
        CLOSED
    }

    public enum TermsType {
        TERMS,
        PRIVACY,
        REFUND
    }

    public enum ServerOpenStatus {
        OPEN,
        MAINTENANCE,
        CLOSED
    }
}
