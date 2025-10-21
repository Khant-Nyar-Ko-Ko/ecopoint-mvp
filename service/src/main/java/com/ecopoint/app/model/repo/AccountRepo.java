package com.ecopoint.app.model.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecopoint.app.model.entity.Account;

public interface AccountRepo extends JpaRepository<Account, Long>{

	Long countByEmail(String email);

	Optional<Account> findByEmail(String email);


}
