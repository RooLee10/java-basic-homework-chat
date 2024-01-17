package ru.li.chat.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InMemoryUserService implements UserService {
    class User {
        private String username;
        private String login;
        private String password;
        private UserRole role;

        public User(String username, String login, String password, UserRole role) {
            this.username = username;
            this.login = login;
            this.password = password;
            this.role = role;
        }
    }

    private List<User> users;

    public InMemoryUserService() {
        this.users = new ArrayList<>(Arrays.asList(
                new User("Admin", "admin", "admin", UserRole.ADMIN),
                new User("User1", "log1", "pass1", UserRole.USER),
                new User("User2", "log2", "pass2", UserRole.USER),
                new User("User3", "log3", "pass3", UserRole.USER)
        ));
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.username;
            }
        }
        return null;
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        for (User user : users) {
            if (user.login.equals(login)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUsernameAlreadyExist(String username) {
        for (User user : users) {
            if (user.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUserAdmin(String username) {
        for (User user : users) {
            if (user.username.equals(username) && user.role == UserRole.ADMIN) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void createNewUser(String username, String login, String password, UserRole role) {
        users.add(new User(username, login, password, role));
    }
}
