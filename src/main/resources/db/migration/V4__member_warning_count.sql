-- 누적 경고 횟수 컬럼 (07-09 피드백: 마이페이지 경고 표시). 기존 유저는 0.
ALTER TABLE public.users
    ADD COLUMN warning_count integer NOT NULL DEFAULT 0;
