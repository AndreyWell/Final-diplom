import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class BooleanSearchEngine implements SearchEngine {
    public static BooleanSearchEngine instance;
    private File pdfsDir;
    private final Map<String, List<PageEntry>> mapWordPageEntry;
    private Set<String> stopWords;

    public Map<String, List<PageEntry>> getMapWordPageEntry() {
        return mapWordPageEntry;
    }

    // Индексация PDF файлов в конструкторе - создание списка слов с данными
    // по частоте их использования на странице каждого файла
    private BooleanSearchEngine(File pdfsDir) throws IOException {
        this.pdfsDir = pdfsDir;
        this.mapWordPageEntry = new HashMap<>();
        this.stopWords = new HashSet<>();

        // Чтение файла со словами исключениями
        try (BufferedReader reader = new BufferedReader(new FileReader("stop-ru.txt"))) {
            String s;
            while ((s = reader.readLine()) != null) {
                stopWords.add(s);
            }
        }

        // Перебор файлов из текущей папки
        for (File filePdf : pdfsDir.listFiles()) {
            // Объект PDF документа
            try (PdfDocument pdfDocument = new PdfDocument(new PdfReader(filePdf))) {
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
                        final List<PageEntry> pageEntryList = new ArrayList<>();
                        String wordFinal = stringIntegerEntry.getKey();
                        Integer count = stringIntegerEntry.getValue();
                        final PageEntry pageEntry = new PageEntry(pdfName, numberPage, count);
                        // Добавление List<PageEntry> в Map<String, List<PageEntry>>, если:
                        // 1) элемент с ключом wordFinal уже есть в Map<String, List<PageEntry>>
                        // 2) это новый элемент для Map<String, List<PageEntry>>
                        if (getMapWordPageEntry().containsKey(wordFinal)) {
                            getMapWordPageEntry().get(wordFinal).add(pageEntry);
                            // Сортировка через реализацию метода compareTo() в PageEntry
                            Collections.sort(getMapWordPageEntry().get(wordFinal));
                        } else {
                            pageEntryList.add(pageEntry);
                            getMapWordPageEntry().put(wordFinal, pageEntryList);
                            // Сортировка через реализацию метода compareTo() в PageEntry
                            Collections.sort(getMapWordPageEntry().get(wordFinal));
                        }
                    }
                }
            }
        }
    }

    // Поиск указанного слова в Map<String, List<PageEntry>>
    @Override
    public List<PageEntry> search(String word) {
        // Список слов за исключением слов из стоп-списка
        List<String> rightWords = new ArrayList<>();
        // Разбиваем полученный текст для поиска на отдельные слова
        String[] split = word.split("\\P{IsAlphabetic}+");
        // Формирование List с разрешенными словами для поиска с исключением слов из стоп-списка
        for (String s : split) {
            if (!stopWords.contains(s)) {
                rightWords.add(s);
            }
        }
        // Промежуточный List для сравнения PageEntry текущего слова с предыдущим List
        List<PageEntry> pageEntries = new ArrayList<>();
        // Итоговый List c PageEntry со сложением количества повторений в поле COUNT
        List<PageEntry> pageEntriesFinal = new ArrayList<>();

        // Получение List c PageEntry по всем указанным словам для поиска с учетом стоп-списка
        for (String rightWord : rightWords) {
            // Если слова для поиска нет в Map<String, List<PageEntry>>
            if (!getMapWordPageEntry().containsKey(rightWord)) {
                continue;
            }
            // Добавление в финальный и промежуточный List полученного PageEntry от первого слова
            if (pageEntries.isEmpty()) {
                pageEntriesFinal.addAll(getMapWordPageEntry().get(rightWord));
                pageEntries.addAll(getMapWordPageEntry().get(rightWord));
            } else {
                // Значение List<PageEntry> для следующих слов, заданных в поиске
                List<PageEntry> pageEntriesNew = new ArrayList<>();
                // Присваиваем экземпляр List<PageEntry> из Map<String, List<PageEntry>> в новый List
                pageEntriesNew.addAll(getMapWordPageEntry().get(rightWord));
                // List для удаления PageEntry в pageEntriesNew, у которых изменилось поле COUNT
                List<PageEntry> deleteNew = new ArrayList<>();
                // Перебор только совпадений PageEntry между List - pageEntriesNew и pageEntriesFinal
                for (PageEntry entry : pageEntries) {
                    for (PageEntry entryExistsNew : pageEntriesNew) {
                        if (entry.getPdfName() == entryExistsNew.getPdfName() &&
                                entry.getPage() == entryExistsNew.getPage()) {
                            // Удаление совпавшего PageEntry
                            pageEntriesFinal.remove(entry);
                            // Добавление того же PageEntry с суммированием количества совпадений (count)
                            pageEntriesFinal.add(new PageEntry(entryExistsNew.getPdfName(), entryExistsNew.getPage(),
                                    entryExistsNew.getCount() + entry.getCount()));
                            // Добавление PageEntry у которых изменился COUNT в список для удаления из pageEntriesNew
                            deleteNew.add(entryExistsNew);
                        }
                    }
                }
                // Удаление совпавших PageEntry из pageEntriesNew, уже добавленных в pageEntriesFinal
                if (!deleteNew.isEmpty()) {
                    for (PageEntry entry : deleteNew) {
                        pageEntriesNew.remove(entry);
                    }
                }
                // Добавление оставшихся уникальных PageEntry из pageEntriesNew в pageEntriesFinal
                if (!pageEntriesNew.isEmpty()) {
                    for (PageEntry entryNew : pageEntriesNew) {
                        pageEntriesFinal.add(entryNew);
                    }
                }
            }
            // Приравнивание промежуточного списка pageEntries с итоговым pageEntriesFinal
            pageEntries.clear();
            pageEntries.addAll(pageEntriesFinal);
        }
        // Сортировка итогового списка
        Collections.sort(pageEntriesFinal);

        if (pageEntriesFinal.isEmpty()) {
            // Ввели только слова исключения
            if (rightWords.isEmpty()) {
            } else {
                // Ввели слова, которых нет
                pageEntriesFinal = getMapWordPageEntry().get(split[0]);
            }
        }
        return pageEntriesFinal;
    }

    public static BooleanSearchEngine getInstance(File pdfsDir) throws IOException {
        if (instance == null) {
            instance = new BooleanSearchEngine(pdfsDir);
        }
        return instance;
    }
}
