package com.ecopoint.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecopoint.app.controller.input.SignUpForm;
import com.ecopoint.app.controller.output.ModificationResult;
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
}
