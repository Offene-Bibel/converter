package parser;

import offeneBibel.parser.AstNode;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.ErrorReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.errors.ErrorUtils;

import static org.testng.Assert.*;

public class ParseTester {
    public static void parseOk(String text) {
        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
        ErrorReportingParseRunner<AstNode> parseRunner = new ErrorReportingParseRunner<AstNode>(parser.Page(), 0);
        ParsingResult<AstNode> result = parseRunner.run(text);
        if ( ! result.isSuccess()) {
            fail(ErrorUtils.printParseErrors(result));
        }
    }

    public static void parseFails(String text) {
        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
        ErrorReportingParseRunner<AstNode> parseRunner = new ErrorReportingParseRunner<AstNode>(parser.Page(), 0);
        ParsingResult<AstNode> result = parseRunner.run(text);
        assertFalse(result.isSuccess());
    }
}

