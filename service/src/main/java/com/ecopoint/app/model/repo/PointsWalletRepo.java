package com.ecopoint.app.model.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecopoint.app.model.entity.PointsWallet;

public interface PointsWalletRepo extends JpaRepository<PointsWallet, Long>{

	Optional<PointsWallet> findByAccount_Id(Long accountId);

}
