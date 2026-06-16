package kr.maribel.backend.repository;

import java.util.Optional;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import kr.maribel.backend.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMicrosoftSub(String microsoftSub);

    Optional<Member> findByMinecraftUuid(String minecraftUuid);

    long countByStatus(UserStatus status);
}
