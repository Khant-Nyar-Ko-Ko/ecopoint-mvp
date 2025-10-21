package com.ecopoint.app.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecopoint.app.controller.input.CloseSessionReq;
import com.ecopoint.app.controller.input.StartSessionReq;
import com.ecopoint.app.controller.output.CloseSessionRes;
import com.ecopoint.app.controller.output.StartSessionRes;
import com.ecopoint.app.exception.BusinessException;
import com.ecopoint.app.model.entity.MachineSession;
import com.ecopoint.app.model.repo.MachineSessionRepo;

@RestController
@RequestMapping("/api/session")
public class SessionApi {
	
	@Autowired
	private MachineSessionRepo sessionRepo;
	
	@PostMapping("/start")
	@Transactional
	public ResponseEntity<StartSessionRes> start(@RequestBody StartSessionReq req) {
		
	    var s = new MachineSession();
	    s.setId(UUID.randomUUID().toString());
	    s.setUserId(req.user_id());          
	    s.setMachineCode(req.machine_code());
	    s.setExpiresAt(LocalDateTime.now().plusMinutes(5));
	    s.setStatus(MachineSession.Status.ACTIVE);
	    s.setCreatedAt(LocalDateTime.now());
	    
	    sessionRepo.save(s);
	    
	    return ResponseEntity.ok(
	    		new StartSessionRes(
	    				s.getId(), 
	    				s.getMachineCode(), 
	    				s.getStatus().name(), 
	    				s.getExpiresAt().toString()
	    		));
	  }

	  @PostMapping("/close")
	  @Transactional
	  public ResponseEntity<CloseSessionRes> close(@RequestBody CloseSessionReq req) {
		  
	    var s = sessionRepo.findById(req.session_id()).orElseThrow(() -> new BusinessException("There is no session"));
	    
	    if (s.getStatus() == MachineSession.Status.ACTIVE) {
	      s.setStatus(MachineSession.Status.CLOSED);
	      s.setClosedAt(LocalDateTime.now());
	      sessionRepo.save(s);
	    }
	    return ResponseEntity.ok(new CloseSessionRes("closed"));
	  }

}
