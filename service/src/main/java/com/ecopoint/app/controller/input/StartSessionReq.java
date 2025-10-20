package com.ecopoint.app.controller.input;

public record StartSessionReq(
		Long user_id,
		String machine_code) {

}
