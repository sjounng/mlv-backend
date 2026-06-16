package kr.maribel.backend.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.api.ApiDtos.MailResponse;
import kr.maribel.backend.api.ApiDtos.WebpanelAckRequest;
import kr.maribel.backend.service.MailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webpanel/mails")
@Tag(name = "Webpanel")
@SecurityRequirement(name = OpenApiConfig.WEBPANEL_KEY)
public class WebpanelController {

    private final MailService mailService;

    public WebpanelController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/pending")
    @Operation(summary = "발송 대기 우편 큐 조회")
    List<MailResponse> pending(@RequestHeader("X-Maribel-Webpanel-Key") String apiKey,
                               @RequestParam(defaultValue = "50") int limit) {
        return mailService.listDispatchable(apiKey, limit).stream().map(DtoMapper::mail).toList();
    }

    @PostMapping("/{id}/ack")
    @Operation(summary = "우편 발송 결과 ACK")
    MailResponse acknowledge(@RequestHeader("X-Maribel-Webpanel-Key") String apiKey,
                             @PathVariable Long id,
                             @Valid @RequestBody WebpanelAckRequest request) {
        return DtoMapper.mail(mailService.acknowledge(apiKey, id, request.status(), request.errorMessage(), request.retryable()));
    }
}
