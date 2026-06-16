package kr.maribel.backend.repository;

import java.util.Optional;
import kr.maribel.backend.domain.MailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailTemplateRepository extends JpaRepository<MailTemplate, Long> {

    Optional<MailTemplate> findByMailCode(String mailCode);
}
