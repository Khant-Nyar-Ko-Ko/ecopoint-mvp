package com.ecopoint.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecopoint.app.controller.input.DepositRequestForm;
import com.ecopoint.app.controller.output.DepositResult;
import com.ecopoint.app.service.MachineService;

@RestController
@RequestMapping("/api/machine")
public class MachineApi {
	
	
	
	@Autowired
	private MachineService service;
	
	
	
	@PostMapping()
	public ResponseEntity<DepositResult> depositRequest(
			@RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
			@RequestBody DepositRequestForm form) {
		
		return ResponseEntity.ok(service.request(form, idemKey));
		
	}

}
