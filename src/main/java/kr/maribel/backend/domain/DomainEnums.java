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

    /** 이벤트 진행 상태(관리자 수동 선택): 진행예정 / 진행중 / 종료 */
    public enum EventStatus {
        UPCOMING,
        ONGOING,
        ENDED
    }

    /** 배너(팝업) 노출 위치: 홈 인트로 슬라이더 / 이벤트 페이지 상단 */
    public enum BannerPlacement {
        HOME,
        EVENT
    }

    /** 상품 구매 제한 유형: 없음 / 매주 월요일 06:00 초기화 / 매월 1일 06:00 초기화 / 계정당 1회 */
    public enum PurchaseLimitType {
        NONE,
        WEEKLY,
        MONTHLY,
        ONCE
    }

    /** 경고 사유 유형: 비매너 / 버그악용 / 직접작성 */
    public enum WarningReason {
        MISCONDUCT,
        BUG_ABUSE,
        CUSTOM
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
        PLAYER_REPORT,
        BUG_REPORT,
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
