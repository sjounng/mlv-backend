-- 문의 분류에 플레이어 신고/버그 신고 추가 (07-09 피드백).
-- 기존 category CHECK 제약을 새 값 포함하도록 교체한다.
ALTER TABLE public.contact_inquiries
    DROP CONSTRAINT IF EXISTS contact_inquiries_category_check;

ALTER TABLE public.contact_inquiries
    ADD CONSTRAINT contact_inquiries_category_check
    CHECK (((category)::text = ANY ((ARRAY[
        'PAYMENT'::character varying,
        'ACCOUNT'::character varying,
        'EVENT'::character varying,
        'PLAYER_REPORT'::character varying,
        'BUG_REPORT'::character varying,
        'OTHER'::character varying
    ])::text[])));
