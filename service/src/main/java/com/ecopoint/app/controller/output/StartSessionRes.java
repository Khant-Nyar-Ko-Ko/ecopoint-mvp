package com.ecopoint.app.controller.output;

public record StartSessionRes(
		String session_id, 
		String machine_code, 
		String status, 
		String expires_at) {

}
