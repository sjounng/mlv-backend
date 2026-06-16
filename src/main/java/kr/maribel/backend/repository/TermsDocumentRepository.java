package kr.maribel.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.DomainEnums.TermsType;
import kr.maribel.backend.domain.TermsDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsDocumentRepository extends JpaRepository<TermsDocument, Long> {

    Optional<TermsDocument> findFirstByTypeAndPublishedAtLessThanEqualOrderByPublishedAtDesc(TermsType type, Instant publishedAt);

    List<TermsDocument> findByTypeOrderByPublishedAtDesc(TermsType type);
}
