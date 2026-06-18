package kr.maribel.backend.service;

import java.time.Instant;
import java.util.List;
import kr.maribel.backend.api.ApiDtos.PopupRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.Popup;
import kr.maribel.backend.repository.PopupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PopupService {

    private final PopupRepository popupRepository;

    public PopupService(PopupRepository popupRepository) {
        this.popupRepository = popupRepository;
    }

    @Transactional(readOnly = true)
    public List<Popup> all() {
        return popupRepository.findAllByOrderByStartAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Popup> visible() {
        return popupRepository.findVisible(Instant.now());
    }

    @Transactional
    public Popup create(PopupRequest request) {
        validatePeriod(request);
        Popup popup = new Popup(request.imageUrl(), request.linkUrl(), request.startAt(), request.endAt());
        popup.setActive(request.active());
        return popupRepository.save(popup);
    }

    @Transactional
    public Popup update(Long id, PopupRequest request) {
        validatePeriod(request);
        Popup popup = get(id);
        popup.update(request.imageUrl(), request.linkUrl(), request.startAt(), request.endAt(), request.active());
        return popup;
    }

    @Transactional
    public Popup setActive(Long id, boolean active) {
        Popup popup = get(id);
        popup.setActive(active);
        return popup;
    }

    private Popup get(Long id) {
        return popupRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("POPUP_NOT_FOUND", "팝업을 찾을 수 없습니다."));
    }

    private void validatePeriod(PopupRequest request) {
        if (request.endAt().isBefore(request.startAt())) {
            throw ApiException.badRequest("INVALID_POPUP_PERIOD", "팝업 종료일은 시작일 이후여야 합니다.");
        }
    }
}
