package com.ecopoint.app.model.repo;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ecopoint.app.model.entity.PointLedger;

public interface PointLedgerRepo extends JpaRepository<PointLedger, Long>{

	List<PointLedger> findByUser_IdOrderByCreateAtDesc(Long userId, PageRequest pageable);

}
