package net.peterv.bazillionaire.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.peterv.bazillionaire.user.models.User;
import net.peterv.bazillionaire.user.models.UserSession;

@ApplicationScoped
public class UserService {

	@Transactional
	public void updateUser(Long id, String username) {
		User user = User.findById(id);
		if (user == null)
			return;
		user.username = username;
	}

	@Transactional
	public void deleteUser(Long id) {
		User user = User.findById(id);
		if (user == null)
			return;
		UserSession.delete("user", user);
		user.delete();
	}
}
