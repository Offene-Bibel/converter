package offeneBibel.parser;

public class ObParallelPassageNode extends ObAstNode {
    private String m_book;
    private int m_chapter;
    private int m_startVerse;
    private int m_stopVerse;

    public ObParallelPassageNode(String book, int chapter, int verse) {
        super(NodeType.parallelPassage);
        m_book = BookNameHelper.getInstance().getUnifiedBookNameForString(book);
        m_chapter = chapter;
        m_startVerse = verse;
        m_stopVerse = -1;
    }

    public ObParallelPassageNode(String book, int chapter, int startVerse, int stopVerse) {
        super(NodeType.parallelPassage);
        m_book = BookNameHelper.getInstance().getUnifiedBookNameForString(book);
        m_chapter = chapter;
        m_startVerse = startVerse;
        m_stopVerse = stopVerse;
    }

    public String getBook() {
        return m_book;
    }

    public int getChapter() {
        return m_chapter;
    }

    public int getStartVerse() {
        return m_startVerse;
    }

    /**
     * Stop verse. Returns -1 if no stop verse was set.
     * @return
     */
    public int getStopVerse() {
        return m_stopVerse;
    }
}
