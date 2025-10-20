package com.ecopoint.app.model.entity;

import com.ecopoint.app.model.RedeemStatus;

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
public class Redemption {
	
	@Id()
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Integer spentPoint;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RedeemStatus status;
	
	@ManyToOne(optional = false)
	private Account user;

}
