package com.ecopoint.app.model.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ecopoint.app.model.RedeemStatus;
import com.ecopoint.app.model.entity.Redemption;

public interface RedemptionRepo extends JpaRepository<Redemption, Long>{

	@Query("""
	        SELECT COALESCE(SUM(r.spentPoint), 0)
	        FROM Redemption r
	        WHERE r.user.id = :userId
	          AND r.status IN (:status1, :status2)
	    """)
	long sumRedeemedAllTime(Long userId, RedeemStatus status1, RedeemStatus status2);

}
