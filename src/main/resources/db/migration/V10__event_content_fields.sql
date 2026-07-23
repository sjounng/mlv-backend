-- 이벤트 목록형 개편 (07-22 피드백): 콘텐츠형 이벤트 필드 추가.
--  - banner_image_url: 목록 썸네일 + 상세 상단 배너
--  - status: 관리자 수동 진행 상태(진행예정/진행중/종료)
--  - featured: 상단 슬라이더 노출 여부
--  - mail_template_id/description: 콘텐츠형 이벤트는 보상 템플릿·본문이 없을 수 있어 NOT NULL 완화
ALTER TABLE public.events ADD COLUMN banner_image_url varchar(500);
ALTER TABLE public.events ADD COLUMN status varchar(20) NOT NULL DEFAULT 'ONGOING';
ALTER TABLE public.events ADD COLUMN featured boolean NOT NULL DEFAULT false;
ALTER TABLE public.events ALTER COLUMN mail_template_id DROP NOT NULL;
ALTER TABLE public.events ALTER COLUMN description DROP NOT NULL;

ALTER TABLE public.events
    ADD CONSTRAINT events_status_check
    CHECK (status IN ('UPCOMING', 'ONGOING', 'ENDED'));
