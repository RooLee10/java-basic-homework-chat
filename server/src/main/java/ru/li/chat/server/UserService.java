package ru.li.chat.server;

public interface UserService {
    String getUsernameByLoginAndPassword(String login, String password);
    boolean isLoginAlreadyExist(String login);
    boolean isUsernameAlreadyExist(String username);
    void createNewUser(String username, String login, String password);
}
