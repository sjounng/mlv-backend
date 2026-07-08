-- 배너(팝업)를 노출 위치별로 구분: HOME(홈 인트로 슬라이더) / EVENT(이벤트 페이지 상단).
-- 기존 배너는 모두 이벤트 배너로 취급하므로 EVENT 로 백필한다.
ALTER TABLE public.popups
    ADD COLUMN placement character varying(30) NOT NULL DEFAULT 'EVENT';

ALTER TABLE public.popups
    ADD CONSTRAINT popups_placement_check
    CHECK (((placement)::text = ANY ((ARRAY['HOME'::character varying, 'EVENT'::character varying])::text[])));

-- 엔티티가 항상 값을 지정하므로 DB 기본값은 제거(스키마를 엔티티 기대와 일치시킨다).
ALTER TABLE public.popups
    ALTER COLUMN placement DROP DEFAULT;
