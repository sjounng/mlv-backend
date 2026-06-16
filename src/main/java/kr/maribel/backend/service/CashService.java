package kr.maribel.backend.service;

import kr.maribel.backend.api.ApiException;
import kr.maribel.backend.domain.CashBalance;
import kr.maribel.backend.domain.CashTransaction;
import kr.maribel.backend.domain.DomainEnums.CashTransactionType;
import kr.maribel.backend.domain.Member;
import kr.maribel.backend.repository.CashBalanceRepository;
import kr.maribel.backend.repository.CashTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashService {

    private final CashBalanceRepository cashBalanceRepository;
    private final CashTransactionRepository cashTransactionRepository;

    public CashService(CashBalanceRepository cashBalanceRepository, CashTransactionRepository cashTransactionRepository) {
        this.cashBalanceRepository = cashBalanceRepository;
        this.cashTransactionRepository = cashTransactionRepository;
    }

    @Transactional(readOnly = true)
    public CashBalance getBalance(Member member) {
        return cashBalanceRepository.findByMemberId(member.getId())
                .orElseThrow(() -> ApiException.notFound("CASH_BALANCE_NOT_FOUND", "캐시 지갑을 찾을 수 없습니다."));
    }

    @Transactional
    public CashBalance credit(Member member, long amount, CashTransactionType type, String refId, String memo) {
        CashBalance balance = getBalanceForUpdate(member);
        balance.credit(amount);
        cashTransactionRepository.save(new CashTransaction(member, type, amount, balance.getBalance(), refId, memo));
        return balance;
    }

    @Transactional
    public CashBalance debit(Member member, long amount, String refId, String memo) {
        CashBalance balance = getBalanceForUpdate(member);
        try {
            balance.debit(amount);
        } catch (IllegalStateException exception) {
            throw ApiException.badRequest("INSUFFICIENT_CASH", "보유 캐시가 부족합니다.");
        }
        cashTransactionRepository.save(new CashTransaction(member, CashTransactionType.SPEND, -amount, balance.getBalance(), refId, memo));
        return balance;
    }

    @Transactional
    public CashBalance deductForRefund(Member member, long amount, String refId, String memo) {
        CashBalance balance = getBalanceForUpdate(member);
        try {
            balance.debit(amount);
        } catch (IllegalStateException exception) {
            throw ApiException.badRequest("INSUFFICIENT_CASH_FOR_REFUND", "환불 처리에 필요한 회수 가능 캐시가 부족합니다.");
        }
        cashTransactionRepository.save(new CashTransaction(member, CashTransactionType.REFUND, -amount, balance.getBalance(), refId, memo));
        return balance;
    }

    private CashBalance getBalanceForUpdate(Member member) {
        return cashBalanceRepository.findByMemberIdForUpdate(member.getId())
                .orElseGet(() -> cashBalanceRepository.save(new CashBalance(member)));
    }
}
