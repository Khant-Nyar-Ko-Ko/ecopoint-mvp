package com.ecopoint.app.controller.input;

import jakarta.validation.constraints.Min;

public record RedeemForm(
		@Min(value = 1) int points,
		String partnerName
		) {

}
