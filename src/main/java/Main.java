import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8989;

        SearchEngine engine = new BooleanSearchEngine(new File("pdfs"));
        Server server = new Server(port, engine);
        server.start();
        System.out.println(engine);
    }
}