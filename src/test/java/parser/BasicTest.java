package parser;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ErrorReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.errors.ErrorUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BasicTest {

    public void parseOk(String text) {
        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
        ErrorReportingParseRunner<ObAstNode> parseRunner = new ErrorReportingParseRunner<ObAstNode>(parser.Page(), 0);
        ParsingResult<ObAstNode> result = parseRunner.run(text);
        if ( ! result.isSuccess()) {
            fail(ErrorUtils.printParseErrors(result));
        }
    }

    public void parseFails(String text) {
        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
        ErrorReportingParseRunner<ObAstNode> parseRunner = new ErrorReportingParseRunner<ObAstNode>(parser.Page(), 0);
        ParsingResult<ObAstNode> result = parseRunner.run(text);
        assertFalse(result.isSuccess());
    }

    @Test
    public void basicPageWorks() {
        parseOk( "{{Lesefassung}}\n\n"
                + "''(kommt später)''\n\n"
                + "{{Studienfassung}}\n\n"
                + "{{S|1}}"
                + "{{Bemerkungen}}\n\n"
                + "{{Kapitelseite Fuß}}\n" );
    }

    @Test
    public void basicPageFails() {
        parseFails( "{{LeFXung}}\n\n"
                + "''(kommt später)''\n\n"
                + "{{Studienfassung}}\n\n"
                + "{{S|1}}"
                + "{{Bemerkungen}}\n\n"
                + "{{Kapitelseite Fuß}}\n" );
    }
}
