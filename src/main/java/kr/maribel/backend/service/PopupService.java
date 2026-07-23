package kr.maribel.backend.service;

import java.time.Instant;
import java.util.List;
import kr.maribel.backend.api.ApiDtos.PopupRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.DomainEnums.BannerPlacement;
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
        return popupRepository.findAllByOrderBySortOrderAscCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Popup> visible() {
        return popupRepository.findVisible(Instant.now());
    }

    /** 특정 노출 위치(HOME/EVENT)의 현재 노출 배너만 조회 */
    @Transactional(readOnly = true)
    public List<Popup> visible(BannerPlacement placement) {
        return popupRepository.findVisibleByPlacement(placement, Instant.now());
    }

    @Transactional
    public Popup create(PopupRequest request) {
        validatePeriod(request);
        Popup popup = new Popup(request.imageUrl(), request.linkUrl(), placementOf(request), request.startAt(), request.endAt());
        popup.setActive(request.active());
        popup.setSortOrder(request.sortOrder());
        return popupRepository.save(popup);
    }

    // 미지정 시 이벤트 배너로 처리(구버전 호환)
    private BannerPlacement placementOf(PopupRequest request) {
        return request.placement() != null ? request.placement() : BannerPlacement.EVENT;
    }

    @Transactional
    public Popup update(Long id, PopupRequest request) {
        validatePeriod(request);
        Popup popup = get(id);
        popup.update(request.imageUrl(), request.linkUrl(), placementOf(request), request.startAt(), request.endAt(), request.sortOrder(), request.active());
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
