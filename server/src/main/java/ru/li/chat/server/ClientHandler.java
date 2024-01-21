package ru.li.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                authentication();
                mainLogic();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void mainLogic() throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                }
                if (message.startsWith("/w ")) {
                    String[] elements = message.split(" "); // /w username message
                    String receiver = elements[1];
                    String msg = elements[2];
                    server.privateMessage(this, receiver, username + " -> " + receiver + ": " + msg);
                    continue;
                }
                if (message.startsWith("/kick ")) {
                    if (!server.getUserService().isUserAdmin(username)) {
                        sendMessage("СЕРВЕР: У вас недостаточно прав");
                        continue;
                    }
                    String[] elements = message.split(" "); // /kick username
                    String usernameToKick = elements[1];
                    server.kickByUsername(usernameToKick);
                    continue;
                }
            }
            server.broadcastMessage(username + ": " + message);
        }
    }

    private void authentication() throws IOException {
        sendMessage("Для аутентификации используйте команду /auth login password.");
        sendMessage("Для регистрации используйте команду /reg username login password.");
        while (true) {
            String message = in.readUTF();
            boolean isSucced = false;
            if (message.startsWith("/auth ")) {
                isSucced = tryToAuthenticate(message);
            } else if (message.startsWith("/reg ")) {
                isSucced = tryToRegister(message);
            } else {
                sendMessage("СЕРВЕР: неизвестная команда");
            }
            if (isSucced) {
                break;
            }
        }
    }

    private boolean tryToRegister(String message) {
        String[] elements = message.split(" "); // /reg username login pass
        if (elements.length != 4) {
            sendMessage("СЕРВЕР: Неккоректная команда регистрации");
            return false;
        }
        String newUsername = elements[1];
        String newLogin = elements[2];
        String newPassword = elements[3];
        if (server.getUserService().isUsernameAlreadyExist(newUsername)) {
            sendMessage("СЕРВЕР: Пользователь с таким именем уже существует");
            return false;
        }
        if (server.getUserService().isLoginAlreadyExist(newLogin)) {
            sendMessage("СЕРВЕР: Пользователь с таким логином уже существует");
            return false;
        }
        server.getUserService().createNewUser(newUsername, newLogin, newPassword, UserRole.USER);
        username = newUsername;
        server.subscribe(this);
        sendMessage("СЕРВЕР: Регистрация прошла успешно");
        sendMessage("СЕРВЕР: Вы подключились к чату под пользователем " + username);
        return true;
    }

    private boolean tryToAuthenticate(String message) {
        String[] elements = message.split(" "); // /auth login pass
        if (elements.length != 3) {
            sendMessage("СЕРВЕР: неккоректная команда аутентификации");
            return false;
        }
        String login = elements[1];
        String password = elements[2];
        String usernameFromUserService = server.getUserService().getUsernameByLoginAndPassword(login, password);
        if (usernameFromUserService == null) {
            sendMessage("СЕРВЕР: Пользователя с указанным логином/паролем не существует");
            return false;
        }
        if (server.isUserBusy(usernameFromUserService)) {
            sendMessage("СЕРВЕР: Учетная запись уже занята");
            return false;
        }
        username = usernameFromUserService;
        server.subscribe(this);
        sendMessage("СЕРВЕР: Вы подключились к чату под пользователем " + username);
        return true;
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
