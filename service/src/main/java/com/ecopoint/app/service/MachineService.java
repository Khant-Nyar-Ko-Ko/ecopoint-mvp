package com.ecopoint.app.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecopoint.app.controller.input.DepositRequestForm;
import com.ecopoint.app.controller.output.DepositResult;
import com.ecopoint.app.exception.BusinessException;
import com.ecopoint.app.model.BottleType;
import com.ecopoint.app.model.LedgerType;
import com.ecopoint.app.model.entity.DepositTxn;
import com.ecopoint.app.model.entity.MachineSession;
import com.ecopoint.app.model.entity.PointLedger;
import com.ecopoint.app.model.repo.AccountRepo;
import com.ecopoint.app.model.repo.DepositTxnzRepo;
import com.ecopoint.app.model.repo.MachineSessionRepo;
import com.ecopoint.app.model.repo.PointLedgerRepo;
import com.ecopoint.app.model.repo.PointsWalletRepo;

@Service
public class MachineService {
	
	@Autowired
	private AccountRepo accountRepo;
	@Autowired
	private PointsWalletRepo walletRepo;
	@Autowired
	private DepositTxnzRepo txnRepo;
	@Autowired
	private MachineSessionRepo sessionRepo;
	@Autowired
	private PointLedgerRepo ledgerRepo;
	
	private static final int POINTS_PER_BOTTLE = 10;
	
	
	@Transactional
	public DepositResult request(DepositRequestForm form, String idemKey) {
		
		if(txnRepo.existsByIdemKey(idemKey)){
			throw new BusinessException("There is already idem key for %s".formatted(idemKey));
		}
		
		
		var session = sessionRepo.findById(form.session_id())
                .orElseThrow(() -> new BusinessException("There is no session"));
		
		if (session.getStatus() != MachineSession.Status.ACTIVE) {
            return DepositResult.rejected("Session is not active");
        }
		
		if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setStatus(MachineSession.Status.EXPIRED);
            sessionRepo.save(session);
            return DepositResult.rejected("Session expired");
        }
		
		if (!session.getMachineCode().equals(form.machine_id())) {
            return DepositResult.rejected("Machine mismatch");
        }
        
		
		if (form.bottle_type() != BottleType.Bottle) {
            return DepositResult.rejected("Only 'Bottle' type is accepted.");
        }
		
		
		
		var user = accountRepo.findById(session.getUserId())
				.orElseThrow(() -> new BusinessException("There is no user Id with %s".formatted(session.getUserId())));

		var wallet = walletRepo.findById(user.getId())
				.orElseThrow(() -> new BusinessException("There is no wallet Id with %s".formatted(user.getId())));
		
		int quantity = Math.max(1, form.quantity());
		int add = quantity * POINTS_PER_BOTTLE;
		
		var txn = new DepositTxn();
		txn.setUser(user);
		txn.setBottleType(form.bottle_type());
		txn.setQuantity(quantity);
		txn.setPointsAdded(add);
		txn.setMachineId(form.machine_id());
		txn.setCreateAt(LocalDateTime.now());
		txn.setIdemKey(idemKey);
		txnRepo.save(txn);
		
		wallet.setBalance(wallet.getBalance() + add);
		walletRepo.save(wallet);
		
		var ledger = new PointLedger();
		ledger.setUser(wallet.getAccount());
		ledger.setType(LedgerType.EARN);
		ledger.setAmount(add);
		ledger.setBalanceAfter(wallet.getBalance().longValue());
		ledger.setCreateAt(LocalDateTime.now());
		ledger.setDepositTxn(txn);
		ledgerRepo.save(ledger);
		
		return DepositResult.success(add, wallet.getBalance().longValue());
	}

	


}
