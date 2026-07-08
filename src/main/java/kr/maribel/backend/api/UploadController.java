package kr.maribel.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.maribel.backend.config.OpenApiConfig;
import kr.maribel.backend.service.ImageStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
@Tag(name = "Admin")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UploadController {

    private final ImageStorageService imageStorageService;

    public UploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "이미지 업로드")
    ApiDtos.UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new ApiDtos.UploadResponse(imageStorageService.store(file));
    }
}
