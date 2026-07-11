-- 런타임 사이트 설정 키-값 (07-10 피드백: 상점 활성화/비활성화 등).
CREATE TABLE public.site_settings (
    setting_key character varying(80) PRIMARY KEY,
    setting_value character varying(500) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);

-- 상점은 기본 활성(세부 UI 확인용). 운영자가 웹패널에서 비활성화 가능.
INSERT INTO public.site_settings (setting_key, setting_value, created_at, updated_at)
VALUES ('shop.enabled', 'true', now(), now());
