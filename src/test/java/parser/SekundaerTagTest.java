package parser;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObAstNode.NodeType;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SekundaerTagTest {
    OffeneBibelParser parser;
    
    @BeforeClass
    public void setup() {
        parser = Parboiled.createParser(OffeneBibelParser.class);
    }
    
    @Test
    public void sekundaerTag() {
        BasicParseRunner<ObAstNode> parseRunner = new BasicParseRunner<ObAstNode>(parser.SecondaryContent());
        assertFalse(parseRunner.run("{{Sekundär}}{{Sekundär ende}}").matched);
        assertFalse(parseRunner.run("{{sekundär}}").matched);
        assertFalse(parseRunner.run("{{sekundär ende}}").matched);
        assertFalse(parseRunner.run("{{Sekundär}}{{Sekundär}}Inhalt{{Sekundär ende}}{{Sekundär ende}}").matched);

        assertTrue(parseRunner.run("{{Sekundär}}Inhalt{{Sekundär ende}}").matched);
        assertTrue(parseRunner.run("{{sekundär}}Inhalt{{sekundär ende}}").matched);
        
        ParsingResult<ObAstNode> result = parseRunner.run("{{Sekundär}}Inhalt{{Sekundär ende}}");
        assertTrue(result.matched);
        assertTrue(result.resultValue.getNodeType() == NodeType.secondaryContent);
        assertTrue(result.resultValue.childCount() == 1);
        assertTrue(result.resultValue.peekChild().getNodeType() == NodeType.text);
        assertTrue(((ObTextNode)result.resultValue.peekChild()).getText().equals("Inhalt"));
    }
}
