package net.peterv.bazillionaire.web;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentSession {
	private String username;

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
}
