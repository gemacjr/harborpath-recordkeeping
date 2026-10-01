package com.harborpath.recordkeeping.web;

public class ApiException extends RuntimeException {
	private final int status;
	private final String error;
	private final String field;

	public ApiException(int status, String error) {
		this(status, error, null);
	}

	public ApiException(int status, String error, String field) {
		super(error);
		this.status = status;
		this.error = error;
		this.field = field;
	}

	public int getStatus() {
		return status;
	}

	public String getError() {
		return error;
	}

	public String getField() {
		return field;
	}

	public static ApiException unauthorized() {
		return new ApiException(401, "That sign-in didn’t match our records.");
	}

	public static ApiException forbidden() {
		return new ApiException(403, "This chair isn’t yours.");
	}

	public static ApiException notFound(String error) {
		return new ApiException(404, error);
	}

	public static ApiException unprocessable(String field, String message) {
		return new ApiException(422, message, field);
	}
}
