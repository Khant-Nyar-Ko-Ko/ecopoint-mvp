package com.ecopoint.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecopoint.app.controller.input.LoginForm;
import com.ecopoint.app.controller.input.SignUpForm;
import com.ecopoint.app.controller.output.ModificationResult;
import com.ecopoint.app.controller.output.UserDetails;
import com.ecopoint.app.controller.output.WalletDetails;
import com.ecopoint.app.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserApi {

	@Autowired
	private UserService userService;
	
	@PostMapping("/create")
	public ResponseEntity<ModificationResult<Long>> create(@RequestBody SignUpForm form) {
		return ResponseEntity.ok(userService.create(form));
	}
	
	@PostMapping("/login")
	public ResponseEntity<ModificationResult<Long>> login(@RequestBody LoginForm form) {
		return ResponseEntity.ok(userService.login(form));
	}
	
	@GetMapping("{id}")
	public ResponseEntity<UserDetails> getUserProfile(@PathVariable Long id) {
		return ResponseEntity.ok(userService.getUserProfile(id));
	}
	
	@GetMapping("/wallet/{id}")
	public ResponseEntity<WalletDetails> getWalletDetails(@PathVariable Long id) {
		return ResponseEntity.ok(userService.getWalletDetails(id));
	}
	
	
}
