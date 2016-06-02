package offeneBibel.parser;

import java.util.List;
import java.util.Vector;

public class ChapterNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private List<ChapterTag> m_chapterTags;

    public ChapterNode() {
        super(NodeType.chapter);
        m_chapterTags = new Vector<ChapterTag>();
    }

    public boolean addChapterTag(ChapterTag tag) {
        if (tag.getTag() == null) return true;
        m_chapterTags.add(tag);
        return true;
    }

    public List<ChapterTag> getChapterTags() {
        return m_chapterTags;
    }
}
