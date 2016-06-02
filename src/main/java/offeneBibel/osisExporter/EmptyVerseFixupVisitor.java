package offeneBibel.osisExporter;

import offeneBibel.parser.AstNode;
import offeneBibel.parser.TextNode;
import offeneBibel.parser.VerseNode;
import offeneBibel.parser.VerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

/**
 * Checks for empty verses and sets the status of those verses to none.
 */
public class EmptyVerseFixupVisitor extends DifferentiatingVisitor<AstNode> implements IVisitor<AstNode>
{
	private boolean verseTextFound = false;
	private VerseNode currentVerse = null;
	
    @Override
    public void visitDefault(AstNode node) throws Throwable
    {
        if(node.getNodeType() == AstNode.NodeType.text) {
            TextNode text = (TextNode)node;
            String textString = text.getText().trim();
            if(false == textString.isEmpty()) {
            	verseTextFound = true;
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.verse) {
        	if(currentVerse != null && verseTextFound == false) {
        		currentVerse.setStatusOverride(VerseStatus.none);
        	}
        	currentVerse = (VerseNode)node;
        	verseTextFound = false;
        }
    }

	@Override protected void visitBeforeDefault(AstNode host) throws Throwable {}
	@Override protected void visitAfterDefault(AstNode host) throws Throwable {}
}
