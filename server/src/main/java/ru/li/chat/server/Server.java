package ru.li.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private int port;
    private Map<String, ClientHandler> clients;
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new HashMap<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            userService = new InDataBaseUserService();
            System.out.println("Запущен сервис для работы с пользователями");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                try {
                    new ClientHandler(this, clientSocket);
                } catch (IOException e) {
                    System.out.println("Не удалось подключить клиента");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clients.values()) {
            clientHandler.sendMessage(message);
        }
    }

    public synchronized void privateMessage(ClientHandler sender, String receiverUsername, String message) {
        ClientHandler receiver = clients.get(receiverUsername);
        if (receiver == null) {
            sender.sendMessage("Не найден пользователь " + receiverUsername);
            return;
        }
        sender.sendMessage(message);
        receiver.sendMessage(message);
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        System.out.println("К чату подключился пользователь " + clientHandler.getUsername());
        broadcastMessage("К чату подключился пользователь " + clientHandler.getUsername());
        clients.put(clientHandler.getUsername(), clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler.getUsername());
        System.out.println("Пользователь " + clientHandler.getUsername() + " покинул чат");
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " покинул чат");
    }

    public synchronized boolean isUserBusy(String username) {
        return clients.containsKey(username);
    }

    public synchronized void kickByUsername(String username) {
        ClientHandler clientHandler = clients.getOrDefault(username, null);
        if (clientHandler != null) {
            clientHandler.disconnect();
        }
    }
}
