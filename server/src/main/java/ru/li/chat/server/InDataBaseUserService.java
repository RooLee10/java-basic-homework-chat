package ru.li.chat.server;

import java.sql.*;
import java.util.*;

public class InDataBaseUserService implements UserService {
    class User {
        private int userId;
        private String userName;
        private String login;
        private String password;
        private Set<UserRole> roles;

        public User(int userId, String userName, String login, String password, Set<UserRole> roles) {
            this.userId = userId;
            this.userName = userName;
            this.login = login;
            this.password = password;
            this.roles = roles;
        }
    }

    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/homework_26";
    private static final String LOGIN = "postgres";
    private static final String PASSWORD = "123456";
    private List<User> users;

    public InDataBaseUserService() {
        this.users = new ArrayList<>();

        String sqlQuery = "SELECT u.user_id, u.user_name, u.login, u.password, r.role_name FROM usertorole utr JOIN users u ON utr.user_id = u.user_id JOIN roles r ON utr.role_id = r.role_id";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, LOGIN, PASSWORD)) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sqlQuery)) {
                    Map<Integer, Map<String, String>> idToUsersData = new HashMap<>(); // Для сохранения данных о пользователях
                    Map<Integer, Set<UserRole>> idToRole = new HashMap<>(); // Для сохранения ролей пользователей
                    while (resultSet.next()) {
                        int userId = resultSet.getInt(1);
                        String userName = resultSet.getString(2);
                        String login = resultSet.getString(3);
                        String password = resultSet.getString(4);
                        String roleName = resultSet.getString(5);
                        // Данные о пользователях
                        if (!idToUsersData.containsKey(userId)) {
                            Map<String, String> userData = new HashMap<>();
                            userData.put("userName", userName);
                            userData.put("login", login);
                            userData.put("password", password);
                            idToUsersData.put(userId, userData);
                        }
                        // Данные о ролях
                        if (idToRole.containsKey(userId)) {
                            Set<UserRole> userRoles = idToRole.get(userId);
                            userRoles.add(UserRole.valueOf(roleName));
                        } else {
                            Set<UserRole> userRoles = new HashSet<>();
                            userRoles.add(UserRole.valueOf(roleName));
                            idToRole.put(userId, userRoles);
                        }
                    }
                    // Обходим сохраненные данные и создаем пользователей
                    for (int userId : idToUsersData.keySet()) {
                        Map<String, String> userData = idToUsersData.get(userId);
                        this.users.add(new User(userId, userData.get("userName"), userData.get("login"), userData.get("password"), idToRole.getOrDefault(userId, new HashSet<>())));
                    }
                }
            } catch (SQLException e) {
                throw new SQLException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.userName;
            }
        }
        return null;
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        for (User user : users) {
            if (user.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUsernameAlreadyExist(String username) {
        for (User user : users) {
            if (user.userName.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUserAdmin(String username) {
        for (User user : users) {
            if (user.userName.equals(username) && user.roles.contains(UserRole.ADMIN)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void createNewUser(String username, String login, String password, UserRole role) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, LOGIN, PASSWORD)) {
            String sqlQuery = "INSERT INTO users (user_name, login, password) values (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
                // Запись в таблице Users
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, login);
                preparedStatement.setString(3, password);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new SQLException(e);
            }
            sqlQuery = "INSERT INTO UserToRole (user_id, role_id) values (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
                // Запись в таблице UserToRole
                // Для записи нам понадобятся первичные ключи из таблицы Users и Roles
                Map<String, Integer> idInfo = getUserIdAndRoleIdByLoginAndRoleName(login, role.toString());
                preparedStatement.setInt(1, idInfo.get("userId"));
                preparedStatement.setInt(2, idInfo.get("roleId"));
                preparedStatement.executeUpdate();
                // Создание пользователя
                Set<UserRole> roles = new HashSet<>();
                roles.add(role);
                this.users.add(new User(idInfo.get("userId"), username, login, password, roles));
            } catch (SQLException | RuntimeException e) {
                throw new SQLException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Integer> getUserIdAndRoleIdByLoginAndRoleName(String login, String roleName) {
        String sqlQuery = "select u.user_id, r.role_id from users u join roles r on u.login = ? and r.role_name = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, LOGIN, PASSWORD)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, roleName);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    resultSet.next();
                    Map<String, Integer> result = new HashMap<>();
                    result.put("userId", resultSet.getInt(1));
                    result.put("roleId", resultSet.getInt(2));
                    return result;
                } catch (SQLException e) {
                    throw new SQLException(e);
                }
            } catch (SQLException e) {
                throw new SQLException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
