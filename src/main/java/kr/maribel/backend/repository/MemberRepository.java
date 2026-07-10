package kr.maribel.backend.repository;

import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.DomainEnums.UserStatus;
import kr.maribel.backend.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMicrosoftSub(String microsoftSub);

    Optional<Member> findByMinecraftUuid(String minecraftUuid);

    long countByStatus(UserStatus status);

    // 악성 유저 일괄 조회: 유효 경고 임계치 이상
    List<Member> findByWarningCountGreaterThanEqualOrderByWarningCountDescCreatedAtDesc(int threshold);

    @Query("""
            select m from Member m
            where (:status is null or m.status = :status)
              and (:keyword = ''
                   or lower(m.minecraftUsername) like lower(concat('%', :keyword, '%'))
                   or lower(m.minecraftUuid) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(m.email, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<Member> search(@Param("status") UserStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);
}
