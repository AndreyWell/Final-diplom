import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8989;

        SearchEngine engine = BooleanSearchEngine.getInstance(new File("pdfs"));
        Server server = new Server(port, engine);
        server.start();
        System.out.println(engine);
    }
}