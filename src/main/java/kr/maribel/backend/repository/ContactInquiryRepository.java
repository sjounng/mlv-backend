package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.ContactInquiry;
import kr.maribel.backend.domain.DomainEnums.ContactStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {

    List<ContactInquiry> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);

    @EntityGraph(attributePaths = "member")
    List<ContactInquiry> findTop50ByOrderByCreatedAtDesc();

    long countByStatus(ContactStatus status);
}
