package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObVerseNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

/**
 * Checks for empty verses and sets the status of those verses to none.
 */
public class EmptyVerseFixupVisitor extends DifferentiatingVisitor<ObAstNode> implements IVisitor<ObAstNode>
{
	private boolean verseTextFound = false;
	private ObVerseNode currentVerse = null;
	
    @Override
    public void visitDefault(ObAstNode node) throws Throwable
    {
        if(node.getNodeType() == ObAstNode.NodeType.text) {
            ObTextNode text = (ObTextNode)node;
            String textString = text.getText().trim();
            if(false == textString.isEmpty()) {
            	verseTextFound = true;
            }
        }

        else if(node.getNodeType() == ObAstNode.NodeType.verse) {
        	if(currentVerse != null && verseTextFound == false) {
        		currentVerse.setStatusOverride(ObVerseStatus.none);
        	}
        	currentVerse = (ObVerseNode)node;
        	verseTextFound = false;
        }
    }

	@Override protected void visitBeforeDefault(ObAstNode host) throws Throwable {}
	@Override protected void visitAfterDefault(ObAstNode host) throws Throwable {}
}
