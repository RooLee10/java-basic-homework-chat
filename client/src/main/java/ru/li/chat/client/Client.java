package ru.li.chat.client;

import java.io.IOException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (Network network = new Network()) {
            network.setOnMessageReceived(arguments -> System.out.println((String) arguments[0]));
            network.connect(8189);
            while (true) {
                String message = scanner.nextLine();
                network.sendMessage(message);
                if (message.equals("/exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
