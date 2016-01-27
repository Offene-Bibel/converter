package offeneBibel.parser;

import java.util.List;
import java.util.Vector;

public class ObChapterNode extends ObAstNode {
    private List<ObChapterTag> m_chapterTags;

    public ObChapterNode() {
        super(NodeType.chapter);
        m_chapterTags = new Vector<ObChapterTag>();
    }

    public boolean addChapterTag(ObChapterTag tag) {
        if (tag.getTag() == null) return true;
        m_chapterTags.add(tag);
        return true;
    }

    public List<ObChapterTag> getChapterTags() {
        return m_chapterTags;
    }
}
