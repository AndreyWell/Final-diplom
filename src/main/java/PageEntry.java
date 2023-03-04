import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class PageEntry implements Comparable<PageEntry>, Serializable {
    @Expose
    private final String pdfName;
    @Expose
    private final int page;
    @Expose
    private final int count;

    public PageEntry(String pdfName, int page, int count) {
        this.pdfName = pdfName;
        this.page = page;
        this.count = count;
    }

    public String getPdfName() {
        return pdfName;
    }

    public int getPage() {
        return page;
    }

    public int getCount() {
        return count;
    }

    @Override
    public int compareTo(PageEntry o) {
        if (count > o.count) {
            return -1;
        } else if (count < o.count) {
            return 1;
        } else {
            return pdfName.compareTo(o.pdfName);
        }
    }
}