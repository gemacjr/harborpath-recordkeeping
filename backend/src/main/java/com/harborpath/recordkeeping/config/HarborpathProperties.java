package com.harborpath.recordkeeping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "harborpath")
public class HarborpathProperties {
	private final Cors cors = new Cors();

	public Cors getCors() {
		return cors;
	}

	public static class Cors {
		private String origins = "http://localhost:4200";

		public String getOrigins() {
			return origins;
		}

		public void setOrigins(String origins) {
			this.origins = origins;
		}
	}
}
