package kr.maribel.backend.api;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.maribel.backend.api.ApiDtos.TermsResponse;
import kr.maribel.backend.domain.DomainEnums.TermsType;
import kr.maribel.backend.service.LegalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/legal")
@Tag(name = "Legal")
public class LegalController {

    private final LegalService legalService;

    public LegalController(LegalService legalService) {
        this.legalService = legalService;
    }

    @GetMapping("/terms/latest")
    @Operation(summary = "최신 약관 문서 조회")
    TermsResponse latest(@RequestParam TermsType type) {
        return DtoMapper.terms(legalService.latest(type));
    }

    @GetMapping("/terms")
    @Operation(summary = "약관 문서 이력 조회")
    List<TermsResponse> history(@RequestParam TermsType type) {
        return legalService.history(type).stream().map(DtoMapper::terms).toList();
    }
}
