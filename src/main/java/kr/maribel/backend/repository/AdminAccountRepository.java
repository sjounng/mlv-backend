package kr.maribel.backend.repository;

import java.util.Optional;
import kr.maribel.backend.domain.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {

    Optional<AdminAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
