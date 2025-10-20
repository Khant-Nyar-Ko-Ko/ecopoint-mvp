package com.ecopoint.app.controller.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpForm(
		 @NotBlank(message = "please enter name")
		 String name,
		 @NotBlank(message = "please enter email")
		 String email,
		 @Size(min = 6)
		 String password
		) {

}
