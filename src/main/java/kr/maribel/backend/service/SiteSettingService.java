package kr.maribel.backend.service;

import kr.maribel.backend.domain.SiteSetting;
import kr.maribel.backend.repository.SiteSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 런타임 사이트 설정 조회/변경. */
@Service
public class SiteSettingService {

    public static final String SHOP_ENABLED = "shop.enabled";

    private final SiteSettingRepository repository;

    public SiteSettingService(SiteSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isShopEnabled() {
        return repository.findById(SHOP_ENABLED)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(true); // 설정 없으면 기본 활성
    }

    @Transactional
    public boolean setShopEnabled(boolean enabled) {
        SiteSetting setting = repository.findById(SHOP_ENABLED)
                .orElseGet(() -> new SiteSetting(SHOP_ENABLED, "true"));
        setting.setValue(Boolean.toString(enabled));
        repository.save(setting);
        return enabled;
    }
}
