package kr.maribel.backend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.maribel.backend.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "mailTemplate"})
    Optional<Product> findWithCategoryAndMailTemplateById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "mailTemplate"})
    @Query("""
            select p from Product p
            where (:categoryId is null or p.category.id = :categoryId)
              and (:activeOnly = false or p.active = true)
              and (:recommended is null or p.recommended = :recommended)
              and (:newBadge is null or p.newBadge = :newBadge)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
            order by p.category.sortOrder asc, p.id desc
            """)
    Page<Product> search(@Param("categoryId") Long categoryId,
                         @Param("activeOnly") boolean activeOnly,
                         @Param("recommended") Boolean recommended,
                         @Param("newBadge") Boolean newBadge,
                         @Param("minPrice") Long minPrice,
                         @Param("maxPrice") Long maxPrice,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}
