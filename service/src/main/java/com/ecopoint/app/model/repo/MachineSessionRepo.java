package com.ecopoint.app.model.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecopoint.app.model.entity.MachineSession;

public interface MachineSessionRepo extends JpaRepository<MachineSession, String>{
	
	Optional<MachineSession> findFirstByMachineCodeAndStatus(String machineCode, MachineSession.Status status);
	
	 Optional<MachineSession> findFirstByUserIdAndMachineCodeAndStatus(
		        Long userId, String machineCode, MachineSession.Status status);

}
