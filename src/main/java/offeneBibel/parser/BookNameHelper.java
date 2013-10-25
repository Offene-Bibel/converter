package offeneBibel.parser;

import java.io.IOException;
import java.util.List;

import util.Misc;

public class BookNameHelper {
    static final String m_bibleBooks = Misc.getResourceDir() + "bibleBooks.txt";
    private List<List<String>> m_bookList;
    private static BookNameHelper m_instance = null;



    private BookNameHelper()
    {
        try {
            m_bookList = Misc.readCsv(m_bibleBooks);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (List<String> bookEntry : m_bookList) {
            bookEntry.remove(2); // remove chapter count
            bookEntry.remove(0); // remove URL name
        }
    }

    public static BookNameHelper getInstance()
    {
        if(m_instance == null) {
            m_instance = new BookNameHelper();
        }
        return m_instance;
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
        for (List<String> book : m_bookList) {
            for (int i = 1; i < book.size(); ++i) {
                if(book.get(i).equals(bookName))
                    return book.get(0);
            }
        }
        return null;
    }
}
