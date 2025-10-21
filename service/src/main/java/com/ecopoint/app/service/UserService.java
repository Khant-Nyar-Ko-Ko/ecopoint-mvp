package com.ecopoint.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecopoint.app.controller.input.LoginForm;
import com.ecopoint.app.controller.input.SignUpForm;
import com.ecopoint.app.controller.output.ModificationResult;
import com.ecopoint.app.controller.output.UserDetails;
import com.ecopoint.app.controller.output.WalletDetails;
import com.ecopoint.app.exception.BusinessException;
import com.ecopoint.app.model.RedeemStatus;
import com.ecopoint.app.model.Role;
import com.ecopoint.app.model.entity.Account;
import com.ecopoint.app.model.entity.PointsWallet;
import com.ecopoint.app.model.repo.AccountRepo;
import com.ecopoint.app.model.repo.DepositTxnzRepo;
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
		wallet.setBalance(0);
		
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
	
	

}
