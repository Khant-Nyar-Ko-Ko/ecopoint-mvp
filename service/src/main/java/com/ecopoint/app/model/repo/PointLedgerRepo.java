package com.ecopoint.app.model.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecopoint.app.model.entity.PointLedger;

public interface PointLedgerRepo extends JpaRepository<PointLedger, Long>{

}
