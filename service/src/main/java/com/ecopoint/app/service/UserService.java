package com.ecopoint.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecopoint.app.controller.input.SignUpForm;
import com.ecopoint.app.controller.output.ModificationResult;
import com.ecopoint.app.exception.BusinessException;
import com.ecopoint.app.model.Role;
import com.ecopoint.app.model.entity.Account;
import com.ecopoint.app.model.entity.PointsWallet;
import com.ecopoint.app.model.repo.AccountRepo;
import com.ecopoint.app.model.repo.PointsWalletRepo;

@Service
public class UserService {
	
	@Autowired
	private AccountRepo accountRepo;
	@Autowired
	private PointsWalletRepo PointsWalletRepo;

	
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
		
		PointsWalletRepo.save(wallet);
		
		return new ModificationResult<>(user.getId(), "Success");
	}

}
