package com.ecopoint.app.model.repo;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ecopoint.app.model.entity.DepositTxn;

public interface DepositTxnzRepo extends JpaRepository<DepositTxn, Long>{

	@Query("""
			SELECT COALESCE(SUM(d.pointsAdded), 0)
			FROM DepositTxn d
            WHERE d.user.id = :userId
            AND d.createAt >= :start
            AND d.createAt < :end
			""")
	long sumPointAddedInRange(Long userId, LocalDateTime start, LocalDateTime end);
	
	
	@Query("""
	        SELECT COALESCE(SUM(d.pointsAdded), 0)
	        FROM DepositTxn d
	        WHERE d.user.id = :userId
	    """)
	    long sumPointsAddedAllTime(Long userId);

}
