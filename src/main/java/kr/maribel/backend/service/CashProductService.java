package kr.maribel.backend.service;

import java.util.List;
import kr.maribel.backend.api.ApiDtos.CashProductUpsertRequest;
import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.CashProduct;
import kr.maribel.backend.repository.CashProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 캐시 충전 상품(패키지) 관리. 공통 상세 소개 본문은 SiteSettingService 로 관리. */
@Service
public class CashProductService {

    private final CashProductRepository repository;

    public CashProductService(CashProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CashProduct> activeProducts() {
        return repository.findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<CashProduct> allProducts() {
        return repository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public CashProduct activeProduct(Long id) {
        CashProduct product = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CASH_PRODUCT_NOT_FOUND", "충전 상품을 찾을 수 없습니다."));
        if (!product.isActive()) {
            throw ApiException.notFound("CASH_PRODUCT_NOT_FOUND", "충전 상품을 찾을 수 없습니다.");
        }
        return product;
    }

    @Transactional
    public CashProduct upsert(Long id, CashProductUpsertRequest request) {
        if (id == null) {
            return repository.save(new CashProduct(request.name(), request.priceKrw(), request.cashAmount(),
                    request.iconUrl(), request.sortOrder(), request.active()));
        }
        CashProduct product = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CASH_PRODUCT_NOT_FOUND", "충전 상품을 찾을 수 없습니다."));
        product.update(request.name(), request.priceKrw(), request.cashAmount(),
                request.iconUrl(), request.sortOrder(), request.active());
        return product;
    }

    @Transactional
    public void delete(Long id) {
        CashProduct product = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CASH_PRODUCT_NOT_FOUND", "충전 상품을 찾을 수 없습니다."));
        repository.delete(product);
    }
}
