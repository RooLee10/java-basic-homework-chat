package ru.li.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private int port;
    private Map<String, ClientHandler> clients;

    public Server(int port) {
        this.port = port;
        this.clients = new HashMap<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                try {
                    subscribe(new ClientHandler(this, clientSocket));
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
            sender.sendMessage("Не найден " + receiverUsername);
            return;
        }
        sender.sendMessage(message);
        receiver.sendMessage(message);
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.put(clientHandler.getUsername(), clientHandler);
        System.out.println("Подключился новый клиент " + clientHandler.getUsername());
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler.getUsername());
        System.out.println("Отключился клиент " + clientHandler.getUsername());
    }
}
