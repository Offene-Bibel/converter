package parser;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObAstNode.NodeType;
import offeneBibel.parser.ObChapterNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.DefaultValueStack;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.ValueStack;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SekundaerTagTest {
    OffeneBibelParser parser;
    BasicParseRunner<ObAstNode> parseRunner;
    @BeforeClass
    public void setup() {
        parser = Parboiled.createParser(OffeneBibelParser.class);
        parseRunner = new BasicParseRunner<ObAstNode>(parser.SecondaryContent());
    }
    
    public void resetValueStack() {
        parseRunner = new BasicParseRunner<ObAstNode>(parser.SecondaryContent());
        ValueStack<ObAstNode> valueStack = new DefaultValueStack<ObAstNode>();
        valueStack.push(new ObChapterNode());
        parseRunner.withValueStack(valueStack);
    }
    
    @Test
    public void sekundaerTag() {
        resetValueStack();
        assertFalse("Content required", parseRunner.run("{{Sekundär}}{{Sekundär ende}}").matched);
        
        resetValueStack();
        assertFalse("End tag missing", parseRunner.run("{{sekundär}}").matched);
        
        resetValueStack();
        assertFalse("{{Sekundär ende}}", parseRunner.run("{{sekundär ende}}").matched);
        
        resetValueStack();
        assertFalse("No direct nesing allowed", parseRunner.run("{{Sekundär}}{{Sekundär}}Inhalt{{Sekundär ende}}{{Sekundär ende}}").matched);

        resetValueStack();
        assertTrue("Standard case should match", parseRunner.run("{{Sekundär}}Inhalt{{Sekundär ende}}").matched);
        
        resetValueStack();
        assertTrue("Standard case small caps should match", parseRunner.run("{{sekundär}}Inhalt{{sekundär ende}}").matched);

        resetValueStack();
        ParsingResult<ObAstNode> result = parseRunner.run("{{Sekundär}}Inhalt{{Sekundär ende}}");
        assertTrue("Standard case capital caps should match", result.matched);
        assertEquals("Shouldn't modify parent", NodeType.chapter, result.resultValue.getNodeType());
        assertEquals("Should add itself to parent", 1, result.resultValue.childCount());
        assertTrue("Should add correct class", result.resultValue.peekChild() instanceof ObAstNode);
        ObAstNode secondaryTag = (ObAstNode)(result.resultValue.peekChild());
        assertEquals("Tag type should be correct", NodeType.secondaryContent, secondaryTag.getNodeType());
        assertEquals("Child should have been added", 1, secondaryTag.childCount());
        assertEquals("Child tag type should match", NodeType.text, secondaryTag.peekChild().getNodeType());
        assertTrue(((ObTextNode)secondaryTag.peekChild()).getText().equals("Inhalt"));
    }
}
