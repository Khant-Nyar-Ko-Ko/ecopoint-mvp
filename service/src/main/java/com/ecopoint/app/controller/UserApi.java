package com.ecopoint.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecopoint.app.controller.input.LoginForm;
import com.ecopoint.app.controller.input.RedeemForm;
import com.ecopoint.app.controller.input.SignUpForm;
import com.ecopoint.app.controller.output.ActivityItem;
import com.ecopoint.app.controller.output.ModificationResult;
import com.ecopoint.app.controller.output.PointsLedger;
import com.ecopoint.app.controller.output.UserDetails;
import com.ecopoint.app.controller.output.WalletDetails;
import com.ecopoint.app.service.UserService;

import jakarta.validation.Valid;

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
	
	@GetMapping("/activity/{id}")
	public ResponseEntity<List<ActivityItem>> getActivity(@PathVariable Long id,@RequestParam(defaultValue = "10") int limit) {
		return ResponseEntity.ok(userService.getActivity(id, limit));
	}
	
	@PostMapping("/redeem/{id}")
	public ResponseEntity<ModificationResult<Long>> redeemedPoint(@PathVariable Long id,@RequestBody @Valid RedeemForm form) {
		return ResponseEntity.ok(userService.redeemedPoint(id, form));
	}
	
	@GetMapping("/get-points/{id}/{sessionId}/{machineid}")
	public ResponseEntity<PointsLedger> getPoints(@PathVariable Long id, @PathVariable String sessionId, @PathVariable String machineid) {
		return ResponseEntity.ok(userService.getPoint(id, sessionId, machineid));
	}
	
}
