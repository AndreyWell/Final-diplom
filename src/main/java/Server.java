import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

public class Server {
    private int port;
    private SearchEngine booleanSearchEngine;
    public Server(int port, SearchEngine booleanSearchEngine) {
        this.port = port;
        this.booleanSearchEngine = booleanSearchEngine;
    }
    public void start() {
        System.out.println("Starting server at " + port + "...");
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket fromClientSocket = serverSocket.accept();
                try (
                        Socket socket = fromClientSocket;
                        // Прием входящих данных от клиента
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        // Отправка данных - ответ сервера
                        PrintWriter out = new PrintWriter(
                                socket.getOutputStream(), true);
                ) {
                    String inputWord;
                    while ((inputWord = in.readLine()) != null) {
                        System.out.println("Получено сервером: " + inputWord);
                        String word = inputWord.toLowerCase();

                        // Поиск слова в PDF
                        List<PageEntry> search = booleanSearchEngine.search(word);
                        String s = gson.toJson(search, List.class);

                        // Условие, когда search = null (слово не найдено)
                        if (Optional.ofNullable(search).isEmpty()) {
                            s = "Слово (-а): " + word.toUpperCase() + " не найдено (-ы)";
                        }

                        // Ввели только слова исключения
                        if (!Optional.ofNullable(search).isEmpty()) {
                            try {
                                search.get(0);
                            } catch (Exception e) {
                                s = "Введено слово (-а) исключение (-я)";
                            }
                        }
                        out.println(s);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Не могу стартовать сервер");
            e.printStackTrace();
        }
    }
}
