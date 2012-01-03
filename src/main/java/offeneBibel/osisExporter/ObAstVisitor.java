package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class ObAstVisitor extends DifferentiatingVisitor<ObAstNode> implements IVisitor<ObAstNode>
{

	@Override
	protected void visitBeforeDefault(ObAstNode host) throws Throwable {
		
	}

	@Override
	protected void visitDefault(ObAstNode host) throws Throwable {
		
	}

	@Override
	protected void visitAfterDefault(ObAstNode host) throws Throwable {
		
	}
}
