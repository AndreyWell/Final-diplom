import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BooleanSearchEngine implements SearchEngine {
    public static BooleanSearchEngine instance;
    private File pdfsDir;
    private PageEntry pageEntry;
    private List<PageEntry> pageEntryList;
    private Map<String, List<PageEntry>> mapWordPageEntry;

    public Map<String, List<PageEntry>> getMapWordPageEntry() {
        return mapWordPageEntry;
    }

    // Индексация PDF файлов в конструкторе - создание списка слов с данными
    // по частоте их использования на странице каждого файла
    private BooleanSearchEngine(File pdfsDir) throws IOException {
        this.pdfsDir = pdfsDir;
        mapWordPageEntry = new HashMap<>();

        // Перебор файлов из текущей папки
        for (File filePdf : pdfsDir.listFiles()) {
            // Объект PDF документа
            PdfDocument pdfDocument = new PdfDocument(new PdfReader(filePdf));
            // Получение имени файла PDF
            String pdfName = filePdf.getName();
            // Получение количества страниц из текущего PDF
            int numberOfPages = pdfDocument.getNumberOfPages();
            System.out.println("pdfName: " + pdfName + ", numberOfPages: " + numberOfPages);
            // Чтение всех страниц
            for (int numberPage = 1; numberPage <= numberOfPages; numberPage++) {
                Map<String, Integer> freqs = new HashMap<>();
                // Получение отдельной страницы
                PdfPage page = pdfDocument.getPage(numberPage);
                // Получить весь текст со страницы
                String text = PdfTextExtractor.getTextFromPage(page);
                // Разбитый текст со страницы на отдельные слова
                String[] wordsOnPage = text.split("\\P{IsAlphabetic}+");
                // Группировка в Map слов (key) с одной страницы с подсчетом количества повторений (value)
                for (String word : wordsOnPage) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    word = word.toLowerCase();
                    freqs.put(word, freqs.getOrDefault(word, 0) + 1);
                }
                // Формирование в Map по каждому слову (key) List из объектов PageEntry (value)
                for (Map.Entry<String, Integer> stringIntegerEntry : freqs.entrySet()) {
                    pageEntryList = new ArrayList<>();
                    String wordFinal = stringIntegerEntry.getKey();
                    Integer count = stringIntegerEntry.getValue();
                    pageEntry = new PageEntry(pdfName, numberPage, count);
                    // Добавление List<PageEntry> в Map<String, List<PageEntry>>, если:
                    // 1) элемент с ключом wordFinal уже есть в Map<String, List<PageEntry>>
                    // 2) это новый элемент для Map<String, List<PageEntry>>
                    if (mapWordPageEntry.containsKey(wordFinal)) {
                        mapWordPageEntry.get(wordFinal).add(pageEntry);
                        // Сортировка через реализацию метода compareTo() в PageEntry
                        Collections.sort(mapWordPageEntry.get(wordFinal));
                    } else {
                        pageEntryList.add(pageEntry);
                        mapWordPageEntry.put(wordFinal, pageEntryList);
                        // Сортировка через реализацию метода compareTo() в PageEntry
                        Collections.sort(mapWordPageEntry.get(wordFinal));
                    }
                }
            }
        }
    }

    // Поиск указанного слова в Map<String, List<PageEntry>>
    @Override
    public List<PageEntry> search(String word) {
        List<PageEntry> pageEntries = mapWordPageEntry.get(word);
        return pageEntries;
    }

    public static BooleanSearchEngine getInstance(File pdfsDir) throws IOException {
        if (instance == null) {
            instance = new BooleanSearchEngine(pdfsDir);
        }
        return instance;
    }
}
