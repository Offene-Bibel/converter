package offeneBibel.parser;

import java.io.IOException;
import java.util.List;

import util.Misc;

public class BookNameHelper {
    static final String bibleBooks = Misc.getResourceDir() + "bibleBooks.txt";
    /*
     * OSIS tag
     * german book name
     * zero or more
     */
    private List<List<String>> bookList;
    private static BookNameHelper instance = null;


    private BookNameHelper()
    {
        try {
            bookList = Misc.readCsv(bibleBooks);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (List<String> bookEntry : bookList) {
            bookEntry.remove(2); // remove chapter count
            bookEntry.remove(0); // remove URL name
        }
    }

    public static BookNameHelper getInstance()
    {
        if(instance == null) {
            instance = new BookNameHelper();
        }
        return instance;
    }

    public boolean isValid(String bookName)
    {
        if(getUnifiedBookNameForString(bookName) == null)
            return false;
        else
            return true;
    }

    public String getUnifiedBookNameForString(String bookName)
    {
        for (List<String> book : bookList) {
            for (int i = 1; i < book.size(); ++i) {
                if(book.get(i).equals(bookName))
                    return book.get(0);
            }
        }
        return null;
    }

    public String getGermanBookNameForOsisId(String osisId)
    {
        for (List<String> book : bookList) {
            if(book.get(0).equals(osisId))
                return book.get(1);
        }
        return null;
    }
}
