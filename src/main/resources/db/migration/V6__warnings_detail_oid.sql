-- V5 에서 warnings.detail 을 text 로 생성했으나, 코드베이스 @Lob String 컨벤션은
-- PostgreSQL oid(대형 객체)로 매핑된다. Hibernate 스키마 검증과 일치하도록 oid 로 재생성.
-- (warnings 테이블은 신규라 데이터가 없어 드롭/추가 안전)
ALTER TABLE public.warnings DROP COLUMN detail;
ALTER TABLE public.warnings ADD COLUMN detail oid NOT NULL;
