package kr.maribel.backend.api;

import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.maribel.backend.api.ApiDtos.PopupResponse;
import kr.maribel.backend.api.ApiDtos.ServerStatusResponse;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.repository.PopupRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public")
public class PublicController {

    private final MaribelProperties properties;
    private final PopupRepository popupRepository;

    public PublicController(MaribelProperties properties, PopupRepository popupRepository) {
        this.properties = properties;
        this.popupRepository = popupRepository;
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
    @Operation(summary = "현재 노출 팝업 조회")
    List<PopupResponse> popups() {
        return popupRepository.findVisible(Instant.now()).stream().map(DtoMapper::popup).toList();
    }
}
