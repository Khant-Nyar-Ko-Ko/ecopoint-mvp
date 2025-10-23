package com.ecopoint.app.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class MachineSession {
	
	
	@Id
	@Column(length = 64) 
	private String id;
	
	@Column(nullable=false)
	private Long userId;
	
	@Column(nullable=false)
	private String machineCode;
	
	@Enumerated(EnumType.STRING) 
	@Column(nullable=false)
	private Status status;
	
	@Column(nullable=false)  
	private LocalDateTime expiresAt;
	
	@Column(nullable=false)  
	private LocalDateTime createdAt;
	
	private LocalDateTime closedAt;
	
	public enum Status { ACTIVE, CLOSED, EXPIRED }

}
