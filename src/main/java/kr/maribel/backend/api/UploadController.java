package kr.maribel.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import kr.maribel.backend.config.MaribelProperties;
import kr.maribel.backend.config.OpenApiConfig;
import org.springframework.util.StringUtils;
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

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final MaribelProperties properties;

    public UploadController(MaribelProperties properties) {
        this.properties = properties;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "이미지 업로드")
    ApiDtos.UploadResponse upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "업로드할 파일이 비어 있습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("INVALID_FILE_TYPE", "이미지 파일만 업로드할 수 있습니다.");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw ApiException.badRequest("INVALID_FILE_TYPE", "지원하지 않는 이미지 형식입니다.");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path dir = Path.of(properties.getUpload().getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to store uploaded file", exception);
        }

        String url = trimTrailingSlash(properties.getUpload().getPublicBaseUrl()) + "/uploads/" + filename;
        return new ApiDtos.UploadResponse(url);
    }

    private String extension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        String ext = StringUtils.getFilenameExtension(originalFilename);
        return ext == null ? "" : ext.toLowerCase();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
