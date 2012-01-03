package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObChapterNode;
import offeneBibel.parser.ObTreeNode;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class ObAstVisitor extends DifferentiatingVisitor<ObTreeNode> implements IVisitor<ObTreeNode>
{
	private String m_studienFassung = "";
	private String m_leseFassung = "";
	private String m_currentFassung = null;
	
	protected void visitBefore(ObAstNode chapter)
	{
		if(chapter.getNodeType() == ObAstNode.NodeType.fassung) {
			// todo: save type of fassung in object so it can be retrieved here
		}
	}
	
	@Override
	protected void visitBeforeDefault(ObTreeNode host) throws Throwable {
		ObAstNode astNode = (ObAstNode)host;
		
	}

	@Override
	protected void visitDefault(ObTreeNode host) throws Throwable {
		ObAstNode astNode = (ObAstNode)host;
		
	}

	@Override
	protected void visitAfterDefault(ObTreeNode host) throws Throwable {
		ObAstNode astNode = (ObAstNode)host;
		
	}

	public String getStudienFassung() {
		return m_studienFassung;
	}

	public String getLeseFassung() {
		return m_leseFassung;
	}
}
