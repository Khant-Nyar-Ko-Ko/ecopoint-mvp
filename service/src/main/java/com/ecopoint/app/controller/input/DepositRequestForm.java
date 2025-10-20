package com.ecopoint.app.controller.input;

import com.ecopoint.app.model.BottleType;

public record DepositRequestForm(
		String session_id,
		String machine_id,
		BottleType bottle_type,
		int quantity
		) {

}
