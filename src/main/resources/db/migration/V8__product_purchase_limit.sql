-- 상품 구매 제한 (07-12 피드백): 유형(NONE/WEEKLY/MONTHLY/ONCE) + 기간 내 구매 가능 횟수.
ALTER TABLE public.products
    ADD COLUMN purchase_limit_type character varying(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN purchase_limit_count integer NOT NULL DEFAULT 1;

ALTER TABLE public.products
    ADD CONSTRAINT products_purchase_limit_type_check
    CHECK (((purchase_limit_type)::text = ANY ((ARRAY[
        'NONE'::character varying,
        'WEEKLY'::character varying,
        'MONTHLY'::character varying,
        'ONCE'::character varying
    ])::text[])));
