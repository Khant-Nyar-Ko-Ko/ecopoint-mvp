package com.ecopoint.app.model.entity;

import java.time.LocalDateTime;

import com.ecopoint.app.model.LedgerType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class PointLedger {
	
	@Id()
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	

    @ManyToOne(optional = false)
	private Account user;
	
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
	private LedgerType type;
	
	@Column(nullable = false)
	private Long amount;
	
	@Column(nullable = false)
	private Long balanceAfter;
	
	
	@Column(nullable = false)
	private LocalDateTime createAt;
	
	@ManyToOne()
	private DepositTxn depositTxn;
	
	@ManyToOne()
	private Redemption redemption;

}
