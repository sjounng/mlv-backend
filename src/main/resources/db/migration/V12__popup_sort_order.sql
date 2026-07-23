-- 배너(팝업) 슬라이더 내 노출 순서 지정 (07-24). 작을수록 먼저 노출.
ALTER TABLE public.popups ADD COLUMN sort_order integer NOT NULL DEFAULT 0;
