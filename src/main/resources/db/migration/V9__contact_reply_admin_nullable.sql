-- 멤버 기반 관리자(admin_users 레코드 없음)의 문의 답변을 허용하기 위해 처리자 컬럼을 nullable 로 완화.
-- (환불의 processed_by 와 동일한 정책 — 점검 M2)
ALTER TABLE public.contact_replies ALTER COLUMN admin_id DROP NOT NULL;
