package com.pachy.highlight.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import java.util.Map;

/**
 * Replace default HTML error rendering with a JSON payload for /error requests.
 * Prevents circular view-path problems when clients expect JSON (e.g. API
 * requests).
 */
@RestController
public class AppErrorController implements ErrorController {

	private final ErrorAttributes errorAttributes;

	public AppErrorController(ErrorAttributes errorAttributes) {
		this.errorAttributes = errorAttributes;
	}

	@RequestMapping("/error")
	public ResponseEntity<Map<String, Object>> error(@NonNull HttpServletRequest request) {
		var opts = ErrorAttributeOptions.defaults()
				.including(ErrorAttributeOptions.Include.MESSAGE)
				.including(ErrorAttributeOptions.Include.BINDING_ERRORS);

		Map<String, Object> attrs = errorAttributes.getErrorAttributes(new ServletWebRequest(request), opts);
		int status = (attrs.get("status") instanceof Integer) ? (Integer) attrs.get("status")
				: HttpStatus.OK.value();

		// sanitize message when configuration disallows exposing details
		Object msg = attrs.getOrDefault("message", "Unexpected error");

		Map<String, Object> body = Map.of(
				"timestamp", attrs.get("timestamp"),
				"status", status,
				"error", attrs.get("error"),
				"message", msg,
				"path", attrs.get("path"));

		return ResponseEntity.status(200).body(body);
	}
}