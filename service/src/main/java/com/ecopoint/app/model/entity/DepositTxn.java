package com.ecopoint.app.model.entity;

import java.time.LocalDateTime;

import com.ecopoint.app.model.BottleType;

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
public class DepositTxn {

	
	@Id()
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(optional = false)
	private Account user;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BottleType bottleType; 
	
	@Column(nullable = false)
	private int quantity;
	
	@Column(nullable = false)
	private long pointsAdded;
	
	@Column(nullable = false)
	private String machineId;
	
	@Column(nullable = false)
	private LocalDateTime createAt;
	
	@Column(unique = true)
    private String idemKey;   
	
}
