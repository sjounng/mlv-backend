package kr.maribel.backend.service;

import kr.maribel.backend.domain.DomainEnums.WarningReason;

/** 경고 사유 유형별 유저 알림 메일 문구 (07-09 피드백). */
final class WarningMailTemplates {

    private static final String FOOTER =
            "\n경고 3회 누적 시 서비스 차단 여부 심의가 진행되며 이의제기를 희망하시는 경우"
            + "\n고객지원 > 직접 문의하기 > 기타 분류태그 선택 후 문의해주시기 바랍니다";

    private WarningMailTemplates() {
    }

    static String subject(WarningReason reason) {
        return switch (reason) {
            case MISCONDUCT -> "비매너 행위 적발 경고";
            case BUG_ABUSE -> "버그악용 적발 경고";
            case CUSTOM -> "경고 안내";
        };
    }

    static String content(WarningReason reason, String customText, int count) {
        String head = switch (reason) {
            case MISCONDUCT -> "최근 비매너 행위가 적발되어 경고가 1회 추가되었습니다 (현재 " + count + "회)";
            case BUG_ABUSE -> "최근 버그악용 행위가 적발되어 경고가 1회 추가되었습니다 (현재 " + count + "회)";
            case CUSTOM -> "최근 [" + (customText == null ? "" : customText) + "] 으로 인해 경고가 1회 추가되었습니다 (현재 " + count + "회)";
        };
        return head + FOOTER;
    }
}
