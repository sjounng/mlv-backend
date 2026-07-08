package kr.maribel.backend.api;

import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.maribel.backend.api.ApiDtos.NoticeResponse;
import kr.maribel.backend.api.ApiDtos.PopupResponse;
import kr.maribel.backend.api.ApiDtos.ServerStatusResponse;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.domain.DomainEnums.BannerPlacement;
import kr.maribel.backend.repository.PopupRepository;
import kr.maribel.backend.service.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public")
public class PublicController {

    private final MaribelProperties properties;
    private final PopupRepository popupRepository;
    private final NoticeService noticeService;

    public PublicController(MaribelProperties properties, PopupRepository popupRepository, NoticeService noticeService) {
        this.properties = properties;
        this.popupRepository = popupRepository;
        this.noticeService = noticeService;
    }

    @GetMapping("/notices")
    @Operation(summary = "공지사항 목록 조회")
    List<NoticeResponse> notices() {
        return noticeService.all().stream().map(DtoMapper::notice).toList();
    }

    @GetMapping("/server-status")
    @Operation(summary = "마리벨 서버 상태 조회")
    ServerStatusResponse serverStatus() {
        return new ServerStatusResponse(
                properties.getServerStatus().getStatus(),
                properties.getServerStatus().getMessage(),
                properties.getServerStatus().getOnlinePlayers()
        );
    }

    @GetMapping("/popups")
    @Operation(summary = "현재 노출 배너 조회 (placement=HOME|EVENT 로 위치 필터)")
    List<PopupResponse> popups(@RequestParam(name = "placement", required = false) BannerPlacement placement) {
        Instant now = Instant.now();
        List<kr.maribel.backend.domain.Popup> popups = placement != null
                ? popupRepository.findVisibleByPlacement(placement, now)
                : popupRepository.findVisible(now);
        return popups.stream().map(DtoMapper::popup).toList();
    }
}
