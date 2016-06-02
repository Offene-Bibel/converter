package offeneBibel.osisExporter;

import offeneBibel.parser.AstNode;
import offeneBibel.parser.AstNode.NodeType;
import offeneBibel.parser.FassungNode;
import offeneBibel.parser.NoteNode;
import offeneBibel.parser.TextNode;
import offeneBibel.parser.VerseNode;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

/**
 * version: sf/lf/ls
 * from: inclusive
 * to: inclusive
 * status: 1-3 # in Arbeit, fast fertig, fertig
 * text
 *
 */

/*
[
	{
		version: "sf"
		from: 1
		to: 1
		status: 2
		text: "Am Anfang sprach Gott: \"Es werde..."
	},
	{}, ...
]
 */
public class WebsiteDbVisitor extends DifferentiatingVisitor<AstNode> implements IVisitor<AstNode>
{
	private String m_currentFassung = "";
	private StringBuilder m_result = new StringBuilder();
    private StringBuilder m_verseText = new StringBuilder();
    private int m_quoteCounter = 0;

    public WebsiteDbVisitor()
    {
    }

    @Override
    public void visitBeforeDefault(AstNode node) throws Throwable
    {
    	if(node.isDescendantOf(NoteNode.class)
    			|| node.isDescendantOf(NodeType.chapterNotes)
    			|| node.isDescendantOf(NodeType.fassungNotes)
    			|| node.isDescendantOf(NodeType.heading)) {
    		// Skip notes and headings.
    		return;
    	}
    	
    	if(node.getNodeType() == AstNode.NodeType.chapter) {
    		m_result.append("[\n");
    	}

    	else if(node.getNodeType() == AstNode.NodeType.fassung) {
        	FassungNode fassung = (FassungNode)node;
        	if(fassung.getFassung() == FassungNode.FassungType.lesefassung) {
        		m_currentFassung = "lf";
        	}
        	else if(fassung.getFassung() == FassungNode.FassungType.studienfassung) {
        		m_currentFassung = "sf";
        	}
        	else {
        		// TODO: Leichte Sprache
        	}
        }

        else if(node.getNodeType() == AstNode.NodeType.quote) {
            if(m_quoteCounter > 0)
            	appendToVerse("»");
            else
            	appendToVerse("„");
            m_quoteCounter++;
        }

        else if(node.getNodeType() == AstNode.NodeType.alternative) {
        	appendToVerse("(");
        }

        else if(node.getNodeType() == AstNode.NodeType.insertion) {
        	appendToVerse("[");
        }

        else if(node.getNodeType() == AstNode.NodeType.omission) {
        	appendToVerse("{");
        }
    }

    @Override
    public void visitDefault(AstNode node) throws Throwable
    {
    	if(node.isDescendantOf(NoteNode.class)
    			|| node.isDescendantOf(NodeType.chapterNotes)
    			|| node.isDescendantOf(NodeType.fassungNotes)
    			|| node.isDescendantOf(NodeType.heading)) {
    		// Skip notes and headings.
    		return;
    	}
    	
        if(node.getNodeType() == AstNode.NodeType.text) {
            TextNode text = (TextNode)node;
            String textString = text.getText();
            appendToVerse(textString.replaceAll("\"", "\\\""));
        }

        else if(node.getNodeType() == AstNode.NodeType.verse) {
        	finishVerse();
        	
            VerseNode verse = (VerseNode)node;
            m_verseText.append("{\n");
            m_verseText.append("version: \"" + m_currentFassung + "\",\n");
            m_verseText.append("from: \"" + verse.getFromNumber() + "\",\n");
            m_verseText.append("to: \"" + verse.getToNumber() + "\",\n");
            m_verseText.append("status: \"" + verse.getStatus().quality() + "\",\n");
            m_verseText.append("text: \"");
        }
    }

    @Override
    public void visitAfterDefault(AstNode node) throws Throwable
    {
    	if(node.isDescendantOf(NoteNode.class)
    			|| node.isDescendantOf(NodeType.chapterNotes)
    			|| node.isDescendantOf(NodeType.fassungNotes)
    			|| node.isDescendantOf(NodeType.heading)) {
    		// Skip notes and headings.
    		return;
    	}
    	
    	if(node.getNodeType() == AstNode.NodeType.chapter) {
    		finishVerse();
    		m_result.append("]");
    	}

    	else if(node.getNodeType() == AstNode.NodeType.quote) {
            m_quoteCounter--;
            if(m_quoteCounter > 0)
            	appendToVerse("«");
            else
            	appendToVerse("“");
        }

        else if(node.getNodeType() == AstNode.NodeType.alternative) {
        	appendToVerse(")");
        }

        else if(node.getNodeType() == AstNode.NodeType.insertion) {
        	appendToVerse("]");
        }

        else if(node.getNodeType() == AstNode.NodeType.omission) {
        	appendToVerse("}");
        }
    }
    
    private void appendToVerse(String text) throws Exception {
    	if(m_verseText.length() == 0) {
            if(text.trim().length() != 0) {
                // Ignore for now.
                // throw new Exception("Text outside verse found: \"" + text + "\".");
            }
    	}
        else {
    	    m_verseText.append(text);
        }
    }

	private void finishVerse() {
		if(m_verseText.length() != 0) {
            // Trim the verse text to suppress verses with trailing newlines.
			m_result.append(m_verseText.toString().trim());
			m_result.append("\"\n},\n");
			m_verseText = new StringBuilder();
		}
	}

    public String getResult() {
        return m_result.toString();
    }
}
