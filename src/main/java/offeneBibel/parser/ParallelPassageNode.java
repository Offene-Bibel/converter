package offeneBibel.parser;

public class ParallelPassageNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private String book;
    private int chapter;
    private int startVerse;
    private int stopVerse;

    public ParallelPassageNode(String book, int chapter, int verse) {
        super(NodeType.parallelPassage);
        this.book = BookNameHelper.getInstance().getUnifiedBookNameForString(book);
        this.chapter = chapter;
        this.startVerse = verse;
        this.stopVerse = -1;
    }

    public ParallelPassageNode(String book, int chapter, int startVerse, int stopVerse) {
        super(NodeType.parallelPassage);
        this.book = BookNameHelper.getInstance().getUnifiedBookNameForString(book);
        this.chapter = chapter;
        this.startVerse = startVerse;
        this.stopVerse = stopVerse;
    }

    public String getOsisBookId() {
        return book;
    }

    public int getChapter() {
        return chapter;
    }

    public int getStartVerse() {
        return startVerse;
    }

    /**
     * Stop verse. Returns -1 if no stop verse was set.
     * @return
     */
    public int getStopVerse() {
        return stopVerse;
    }
}
