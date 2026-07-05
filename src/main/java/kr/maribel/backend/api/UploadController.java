package kr.maribel.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read uploaded file", exception);
        }
        // Content-Type/확장자는 클라이언트가 위조할 수 있으므로 파일 시그니처(매직바이트)로 실제 형식을 검증한다.
        if (!signatureMatchesExtension(bytes, extension)) {
            throw ApiException.badRequest("INVALID_FILE_TYPE", "파일 내용이 이미지 형식과 일치하지 않습니다.");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path dir = Path.of(properties.getUpload().getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to store uploaded file", exception);
        }

        String url = trimTrailingSlash(properties.getUpload().getPublicBaseUrl()) + "/uploads/" + filename;
        return new ApiDtos.UploadResponse(url);
    }

    private boolean signatureMatchesExtension(byte[] bytes, String extension) {
        return switch (extension) {
            case "png" -> startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "gif" -> startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            case "webp" -> bytes.length >= 12
                    && startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
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
