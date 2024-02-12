package ru.li.chat.server;

import java.sql.*;
import java.util.*;

public class InDataBaseUserService implements UserService {
    class User {
        private String userName;
        private String login;
        private String password;
        private Set<UserRole> roles;

        public User(String userName, String login, String password, Set<UserRole> roles) {
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
        fillUsers();
    }

    private void fillUsers() {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, LOGIN, PASSWORD)) {
            getUsersFromDataBase(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void getUsersFromDataBase(Connection connection) throws SQLException {
        String sqlQuery = "SELECT u.user_id, u.user_name, u.login, u.password, r.role_name FROM UserToRole utr JOIN Users u ON utr.user_id = u.user_id JOIN Roles r ON utr.role_id = r.role_id";
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sqlQuery)) {
                Map<Integer, Map<String, String>> idToUsersData = new HashMap<>();
                Map<Integer, Set<UserRole>> idToRole = new HashMap<>();
                while (resultSet.next()) {
                    int userId = resultSet.getInt(1);
                    String userName = resultSet.getString(2);
                    String login = resultSet.getString(3);
                    String password = resultSet.getString(4);
                    String roleName = resultSet.getString(5);
                    if (!idToUsersData.containsKey(userId)) {
                        Map<String, String> userData = new HashMap<>();
                        userData.put("userName", userName);
                        userData.put("login", login);
                        userData.put("password", password);
                        idToUsersData.put(userId, userData);
                    }
                    if (idToRole.containsKey(userId)) {
                        Set<UserRole> userRoles = idToRole.get(userId);
                        userRoles.add(UserRole.valueOf(roleName));
                    } else {
                        Set<UserRole> userRoles = new HashSet<>();
                        userRoles.add(UserRole.valueOf(roleName));
                        idToRole.put(userId, userRoles);
                    }
                }
                for (int userId : idToUsersData.keySet()) {
                    Map<String, String> userData = idToUsersData.get(userId);
                    this.users.add(new User(userData.get("userName"), userData.get("login"), userData.get("password"), idToRole.getOrDefault(userId, new HashSet<>())));
                }
            } catch (SQLException e) {
                throw new SQLException(e);
            }
        } catch (SQLException e) {
            throw new SQLException(e);
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
            connection.setAutoCommit(false);
            insertIntoUsers(username, login, password, connection);
            insertIntoUserToRole(login, role, connection);
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Set<UserRole> roles = new HashSet<>(Arrays.asList(role));
        this.users.add(new User(username, login, password, roles));
    }

    private void insertIntoUserToRole(String login, UserRole role, Connection connection) throws SQLException {
        String sqlQuery = "INSERT INTO UserToRole (user_id, role_id) values (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
            Map<String, Integer> idInfo = getUserIdAndRoleIdByLoginAndRoleName(login, role.toString(), connection);
            preparedStatement.setInt(1, idInfo.get("userId"));
            preparedStatement.setInt(2, idInfo.get("roleId"));
            preparedStatement.executeUpdate();
        } catch (SQLException | RuntimeException e) {
            throw new SQLException(e);
        }
    }

    private static void insertIntoUsers(String username, String login, String password, Connection connection) throws SQLException {
        String sqlQuery = "INSERT INTO Users (user_name, login, password) values (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, login);
            preparedStatement.setString(3, password);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    private Map<String, Integer> getUserIdAndRoleIdByLoginAndRoleName(String login, String roleName, Connection connection) throws SQLException {
        String sqlQuery = "SELECT u.user_id, r.role_id FROM Users u join Roles r on u.login = ? and r.role_name = ?";
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
    }
}
