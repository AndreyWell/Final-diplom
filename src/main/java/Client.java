import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws Exception {
        try (Socket clientSocket = new Socket("localhost", 8989);
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream()));
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(System.in))
        ) {
            while (true) {
                System.out.println("Введите слово для поиска или 'end':");

                String input = reader.readLine();

                if (input.equals("end")) {
                    out.println("end");
                    break;
                }

                // Отправка запроса серверу
                out.println(input);
                // Ответ сервера
                String answer = in.readLine();
                System.out.println(answer);
            }
        }
    }
}
