package com.ecopoint.app.controller.output;

import java.time.LocalDateTime;

public record ActivityItem(
		String type,
		long points,
		LocalDateTime at
		) {

}
