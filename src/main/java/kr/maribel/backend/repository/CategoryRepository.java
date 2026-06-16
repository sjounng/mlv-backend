package kr.maribel.backend.repository;

import java.util.List;
import java.util.Optional;
import kr.maribel.backend.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    Optional<Category> findByName(String name);
}
