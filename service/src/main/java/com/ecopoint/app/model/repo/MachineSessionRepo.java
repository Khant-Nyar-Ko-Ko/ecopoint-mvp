package com.ecopoint.app.model.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecopoint.app.model.entity.MachineSession;

public interface MachineSessionRepo extends JpaRepository<MachineSession, String>{

}
