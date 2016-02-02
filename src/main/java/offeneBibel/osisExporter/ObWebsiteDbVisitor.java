package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObAstNode.NodeType;
import offeneBibel.parser.ObFassungNode;
import offeneBibel.parser.ObNoteNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObVerseNode;
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
public class ObWebsiteDbVisitor extends DifferentiatingVisitor<ObAstNode> implements IVisitor<ObAstNode>
{
	private String m_currentFassung = "";
	private StringBuilder m_result = new StringBuilder();
    private StringBuilder m_verseText = new StringBuilder();
    private int m_quoteCounter = 0;

    public ObWebsiteDbVisitor()
    {
    }

    @Override
    public void visitBeforeDefault(ObAstNode node) throws Throwable
    {
    	if(node.isDescendantOf(ObNoteNode.class)
    			|| node.isDescendantOf(NodeType.chapterNotes)
    			|| node.isDescendantOf(NodeType.fassungNotes)
    			|| node.isDescendantOf(NodeType.heading)) {
    		// Skip notes and headings.
    		return;
    	}
    	
    	if(node.getNodeType() == ObAstNode.NodeType.chapter) {
    		m_result.append("[\n");
    	}

    	else if(node.getNodeType() == ObAstNode.NodeType.fassung) {
        	ObFassungNode fassung = (ObFassungNode)node;
        	if(fassung.getFassung() == ObFassungNode.FassungType.lesefassung) {
        		m_currentFassung = "lf";
        	}
        	else if(fassung.getFassung() == ObFassungNode.FassungType.studienfassung) {
        		m_currentFassung = "sf";
        	}
        	else {
        		// TODO: Leichte Sprache
        	}
        }

        else if(node.getNodeType() == ObAstNode.NodeType.quote) {
            if(m_quoteCounter > 0)
            	appendToVerse("»");
            else
            	appendToVerse("„");
            m_quoteCounter++;
        }

        else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
        	appendToVerse("(");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
        	appendToVerse("[");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.omission) {
        	appendToVerse("{");
        }
    }

    @Override
    public void visitDefault(ObAstNode node) throws Throwable
    {
    	if(node.isDescendantOf(ObNoteNode.class)
    			|| node.isDescendantOf(NodeType.chapterNotes)
    			|| node.isDescendantOf(NodeType.fassungNotes)
    			|| node.isDescendantOf(NodeType.heading)) {
    		// Skip notes and headings.
    		return;
    	}
    	
        if(node.getNodeType() == ObAstNode.NodeType.text) {
            ObTextNode text = (ObTextNode)node;
            String textString = text.getText();
            appendToVerse(textString.replaceAll("\"", "\\\""));
        }

        else if(node.getNodeType() == ObAstNode.NodeType.verse) {
        	finishVerse();
        	
            ObVerseNode verse = (ObVerseNode)node;
            m_verseText.append("{\n");
            m_verseText.append("version: \"" + m_currentFassung + "\",\n");
            m_verseText.append("from: \"" + verse.getFromNumber() + "\",\n");
            m_verseText.append("to: \"" + verse.getToNumber() + "\",\n");
            m_verseText.append("status: \"" + verse.getStatus().quality() + "\",\n");
            m_verseText.append("text: \"");
        }
    }

    @Override
    public void visitAfterDefault(ObAstNode node) throws Throwable
    {
    	if(node.isDescendantOf(ObNoteNode.class)
    			|| node.isDescendantOf(NodeType.chapterNotes)
    			|| node.isDescendantOf(NodeType.fassungNotes)
    			|| node.isDescendantOf(NodeType.heading)) {
    		// Skip notes and headings.
    		return;
    	}
    	
    	if(node.getNodeType() == ObAstNode.NodeType.chapter) {
    		finishVerse();
    		m_result.append("]");
    	}

    	else if(node.getNodeType() == ObAstNode.NodeType.quote) {
            m_quoteCounter--;
            if(m_quoteCounter > 0)
            	appendToVerse("«");
            else
            	appendToVerse("“");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
        	appendToVerse(")");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
        	appendToVerse("]");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.omission) {
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
