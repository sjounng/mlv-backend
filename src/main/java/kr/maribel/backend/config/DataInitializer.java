package kr.maribel.backend.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import kr.maribel.backend.domain.AdminAccount;
import kr.maribel.backend.domain.Category;
import kr.maribel.backend.domain.DomainEnums.EventType;
import kr.maribel.backend.domain.DomainEnums.Role;
import kr.maribel.backend.domain.DomainEnums.TermsType;
import kr.maribel.backend.domain.MailTemplate;
import kr.maribel.backend.domain.MaribelEvent;
import kr.maribel.backend.domain.Product;
import kr.maribel.backend.domain.TermsDocument;
import kr.maribel.backend.repository.AdminAccountRepository;
import kr.maribel.backend.repository.CategoryRepository;
import kr.maribel.backend.repository.MailTemplateRepository;
import kr.maribel.backend.repository.MaribelEventRepository;
import kr.maribel.backend.repository.ProductRepository;
import kr.maribel.backend.repository.TermsDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MaribelProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final AdminAccountRepository adminAccountRepository;
    private final CategoryRepository categoryRepository;
    private final MailTemplateRepository mailTemplateRepository;
    private final ProductRepository productRepository;
    private final MaribelEventRepository eventRepository;
    private final TermsDocumentRepository termsDocumentRepository;

    public DataInitializer(MaribelProperties properties,
                           PasswordEncoder passwordEncoder,
                           AdminAccountRepository adminAccountRepository,
                           CategoryRepository categoryRepository,
                           MailTemplateRepository mailTemplateRepository,
                           ProductRepository productRepository,
                           MaribelEventRepository eventRepository,
                           TermsDocumentRepository termsDocumentRepository) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.adminAccountRepository = adminAccountRepository;
        this.categoryRepository = categoryRepository;
        this.mailTemplateRepository = mailTemplateRepository;
        this.productRepository = productRepository;
        this.eventRepository = eventRepository;
        this.termsDocumentRepository = termsDocumentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedTerms();
        seedShopAndEvents();
    }

    private void seedAdmin() {
        String username = properties.getBootstrapAdmin().getUsername();
        if (adminAccountRepository.existsByUsername(username)) {
            return;
        }
        adminAccountRepository.save(new AdminAccount(
                username,
                passwordEncoder.encode(properties.getBootstrapAdmin().getPassword()),
                Role.SUPER_ADMIN
        ));
        log.warn("Bootstrap admin account created. username={} Change MARIBEL_BOOTSTRAP_ADMIN_PASSWORD before production.", username);
    }

    private void seedTerms() {
        Instant publishedAt = Instant.parse("2026-05-19T00:00:00Z");
        if (termsDocumentRepository.findByTypeOrderByPublishedAtDesc(TermsType.TERMS).isEmpty()) {
            termsDocumentRepository.save(new TermsDocument(TermsType.TERMS, "v1.0", "마리벨 이용약관 초안입니다. 운영 전 정식 약관으로 교체하세요.", publishedAt));
        }
        if (termsDocumentRepository.findByTypeOrderByPublishedAtDesc(TermsType.PRIVACY).isEmpty()) {
            termsDocumentRepository.save(new TermsDocument(TermsType.PRIVACY, "v1.0", "마리벨 개인정보처리방침 초안입니다. 운영 전 정식 방침으로 교체하세요.", publishedAt));
        }
        if (termsDocumentRepository.findByTypeOrderByPublishedAtDesc(TermsType.REFUND).isEmpty()) {
            termsDocumentRepository.save(new TermsDocument(TermsType.REFUND, "v1.0", "마리벨 환불 정책 초안입니다. 운영 전 정식 정책으로 교체하세요.", publishedAt));
        }
    }

    private void seedShopAndEvents() {
        Category starter = categoryRepository.findByName("스타터")
                .orElseGet(() -> categoryRepository.save(new Category("스타터", 10)));

        MailTemplate starterKit = mailTemplateRepository.findByMailCode("STARTER_KIT")
                .orElseGet(() -> mailTemplateRepository.save(new MailTemplate(
                        "STARTER_KIT",
                        "마리벨 스타터 패키지",
                        "웹상점에서 구매한 스타터 패키지입니다.",
                        "[{\"itemId\":\"minecraft:bread\",\"quantity\":16},{\"itemId\":\"minecraft:iron_pickaxe\",\"quantity\":1}]"
                )));

        MailTemplate attendance = mailTemplateRepository.findByMailCode("ATTENDANCE_DAILY")
                .orElseGet(() -> mailTemplateRepository.save(new MailTemplate(
                        "ATTENDANCE_DAILY",
                        "출석 보상",
                        "오늘의 출석 보상입니다.",
                        "[{\"itemId\":\"minecraft:emerald\",\"quantity\":1}]"
                )));

        if (productRepository.count() == 0) {
            Product product = new Product(
                    "스타터 패키지",
                    "초반 정착에 필요한 기본 아이템 묶음입니다.",
                    1000,
                    "/assets/products/starter-kit.png",
                    starter,
                    starterKit
            );
            product.update(product.getName(), product.getDescription(), product.getPrice(), product.getImageUrl(),
                    starter, starterKit, true, null, true, true,
                    kr.maribel.backend.domain.DomainEnums.PurchaseLimitType.NONE, 1);
            productRepository.save(product);
        }

        if (eventRepository.count() == 0) {
            eventRepository.save(new MaribelEvent(
                    "매일 출석 체크",
                    EventType.ATTENDANCE,
                    "하루 한 번 출석 보상을 받을 수 있습니다.",
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    Instant.now().plus(365, ChronoUnit.DAYS),
                    attendance
            ));
        }
    }
}
