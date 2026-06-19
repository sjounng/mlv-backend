package kr.maribel.backend.service;

import java.util.List;
import kr.maribel.backend.api.ApiDtos.NoticeRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.Notice;
import kr.maribel.backend.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Transactional(readOnly = true)
    public List<Notice> all() {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc();
    }

    @Transactional
    public Notice create(NoticeRequest request) {
        return noticeRepository.save(new Notice(request.title(), request.content(), request.pinned()));
    }

    @Transactional
    public Notice update(Long id, NoticeRequest request) {
        Notice notice = get(id);
        notice.update(request.title(), request.content(), request.pinned());
        return notice;
    }

    @Transactional
    public void delete(Long id) {
        noticeRepository.delete(get(id));
    }

    private Notice get(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다."));
    }
}
