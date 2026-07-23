package kr.maribel.backend.repository;

import java.util.List;
import kr.maribel.backend.domain.CashProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashProductRepository extends JpaRepository<CashProduct, Long> {

    // 공개용: 활성 상품을 정렬순서 → id 순
    List<CashProduct> findByActiveTrueOrderBySortOrderAscIdAsc();

    // 관리자용: 전체를 정렬순서 → id 순
    List<CashProduct> findAllByOrderBySortOrderAscIdAsc();
}
