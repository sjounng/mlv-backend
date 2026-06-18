package kr.maribel.backend.service;

import java.time.Instant;
import java.util.List;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.DomainEnums.TermsType;
import kr.maribel.backend.domain.TermsDocument;
import kr.maribel.backend.repository.TermsDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalService {

    private final TermsDocumentRepository termsDocumentRepository;

    public LegalService(TermsDocumentRepository termsDocumentRepository) {
        this.termsDocumentRepository = termsDocumentRepository;
    }

    @Transactional(readOnly = true)
    public TermsDocument latest(TermsType type) {
        return termsDocumentRepository.findFirstByTypeAndPublishedAtLessThanEqualOrderByPublishedAtDesc(type, Instant.now())
                .orElseThrow(() -> ApiException.notFound("TERMS_NOT_FOUND", "게시된 약관 문서를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<TermsDocument> history(TermsType type) {
        return termsDocumentRepository.findByTypeOrderByPublishedAtDesc(type);
    }

    @Transactional(readOnly = true)
    public List<TermsDocument> all() {
        return termsDocumentRepository.findAllByOrderByPublishedAtDesc();
    }

    @Transactional
    public TermsDocument publish(TermsType type, String version, String content) {
        return termsDocumentRepository.save(new TermsDocument(type, version, content, Instant.now()));
    }
}
