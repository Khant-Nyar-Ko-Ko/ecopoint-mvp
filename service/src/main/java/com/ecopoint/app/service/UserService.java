package com.ecopoint.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecopoint.app.controller.input.LoginForm;
import com.ecopoint.app.controller.input.RedeemForm;
import com.ecopoint.app.controller.input.SignUpForm;
import com.ecopoint.app.controller.output.ActivityItem;
import com.ecopoint.app.controller.output.ModificationResult;
import com.ecopoint.app.controller.output.UserDetails;
import com.ecopoint.app.controller.output.WalletDetails;
import com.ecopoint.app.exception.BusinessException;
import com.ecopoint.app.model.LedgerType;
import com.ecopoint.app.model.RedeemStatus;
import com.ecopoint.app.model.Role;
import com.ecopoint.app.model.entity.Account;
import com.ecopoint.app.model.entity.PointLedger;
import com.ecopoint.app.model.entity.PointsWallet;
import com.ecopoint.app.model.entity.Redemption;
import com.ecopoint.app.model.repo.AccountRepo;
import com.ecopoint.app.model.repo.DepositTxnzRepo;
import com.ecopoint.app.model.repo.PointLedgerRepo;
import com.ecopoint.app.model.repo.PointsWalletRepo;
import com.ecopoint.app.model.repo.RedemptionRepo;

@Service
public class UserService {
	
	@Autowired
	private AccountRepo accountRepo;
	@Autowired
	private PointsWalletRepo pointsWalletRepo;
	@Autowired
	private DepositTxnzRepo depositTxnzRepo;
	@Autowired
	private RedemptionRepo redemptionRepo;
	@Autowired
	private PointLedgerRepo ledgerRepo;

	
	@Transactional
	public ModificationResult<Long> create(SignUpForm form) {
		
		
		if(accountRepo.countByEmail(form.email()) > 0) {
			throw new BusinessException("There is already email with %s".formatted(form.email()));
		}
		
		var user = new Account();
		user.setName(form.name());
		user.setEmail(form.email());
		user.setPassword(form.password());
		user.setRole(Role.USER);
		
		accountRepo.save(user);
		
		var wallet = new PointsWallet();
		wallet.setAccount(user);
		wallet.setBalance(1000);
		
		pointsWalletRepo.save(wallet);
		
		return new ModificationResult<>(user.getId(), "Success");
	}

	@Transactional(readOnly = true)
	public ModificationResult<Long> login(LoginForm form) {
		
		var account = accountRepo.findByEmail(form.email())
				.orElseThrow(() -> new BusinessException("There is no account with this email".formatted(form.email())));
		
		if(account.getPassword().equals(form.password())) {
			return new ModificationResult<Long>(account.getId(), "Success");
		}
		
		throw new BusinessException("Invalid password");
	}

	@Transactional(readOnly = true)
	public UserDetails getUserProfile(Long id) {
		
		var account = accountRepo.findById(id)
				.orElseThrow(() -> new BusinessException("There is no account with this user id : %s".formatted(id)));
		
		
		return new UserDetails(account.getName(), account.getEmail());
	}

	@Transactional(readOnly = true)
	public WalletDetails getWalletDetails(Long id) {
		
		accountRepo.findById(id)
				.orElseThrow(() -> new BusinessException("There is no account with this user id : %s".formatted(id)));
		
		long currentWallet = pointsWalletRepo.findByAccount_Id(id)
				.map(w -> w.getBalance().longValue())
				.orElse(0L);
		
		LocalDate today = LocalDate.now();
		LocalDateTime startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
		LocalDateTime startOfNextMonth = today.with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay();
		
		long earnedThisMonth = depositTxnzRepo.sumPointAddedInRange(id, startOfMonth, startOfNextMonth);
		long totalEarned = depositTxnzRepo.sumPointsAddedAllTime(id);
		
		long totalRedeemed = redemptionRepo.sumRedeemedAllTime(
				id, RedeemStatus.APPROVED, RedeemStatus.FULFILLED);
		
		return new WalletDetails(currentWallet, earnedThisMonth, totalRedeemed, totalEarned);
	}

	@Transactional(readOnly = true)
	public List<ActivityItem> getActivity(Long userId, int limit) {
		
		
		var datas = ledgerRepo.findByUser_IdOrderByCreateAtDesc(userId, PageRequest.of(0, limit));
		
		if(datas.isEmpty()) {
			throw new BusinessException("There is no activity for user id : %s".formatted(userId));
		}
		
		return datas.stream()
				.map(data -> new ActivityItem(data.getType().name(), data.getAmount(), data.getCreateAt())).toList();
	}

	@Transactional
	public ModificationResult<Long> redeemedPoint(Long id, RedeemForm form) {
		
		var account = accountRepo.findById(id)
									.orElseThrow(() -> new BusinessException("There is no account with this user id : %s".formatted(id)));
		
		var wallet = pointsWalletRepo.findById(account.getId()).get();
		
		if( form.points() > wallet.getBalance() ) {
			throw new BusinessException("You don't have enough points to redeem");
		}
		
		wallet.setBalance(wallet.getBalance() - form.points());
		pointsWalletRepo.save(wallet);
		
		
		var redemption = new Redemption();
		redemption.setSpentPoint(form.points());
		redemption.setPartner(form.partnerName());
		redemption.setStatus(RedeemStatus.APPROVED);
		redemption.setCreateAt(LocalDateTime.now());
		redemption.setUser(account);
		
		redemptionRepo.save(redemption);
		
		var ledger = new PointLedger();
		ledger.setUser(account);
		ledger.setAmount(form.points());
		ledger.setBalanceAfter(wallet.getBalance());
		ledger.setCreateAt(LocalDateTime.now());
		ledger.setRedemption(redemption);
		ledger.setType(LedgerType.REDEEM);
		
		ledgerRepo.save(ledger);
		
		return new ModificationResult<Long>(id, "success");
	}
	
	

}
