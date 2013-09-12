package offeneBibel.validator;

import java.io.IOException;
import java.net.URLEncoder;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;

import util.Misc;

import com.beust.jcommander.JCommander;

public class Validator {
    CommandLineArguments m_commandLineArguments;

    public static void main(String[] args) {
        Validator validator = new Validator();
        validator.run(args);
    }

    public void run(String[] args) {
        m_commandLineArguments = new CommandLineArguments();
        JCommander commander = new JCommander(m_commandLineArguments, args);
        if (m_commandLineArguments.m_help) {
            commander.usage();
            return;
        }

        if((m_commandLineArguments.m_inputFile != null && m_commandLineArguments.m_inputUrl != null) ||
           (m_commandLineArguments.m_inputFile == null && m_commandLineArguments.m_inputUrl == null)) {
            System.out.println("Specify either a file *or* a URL, not both.");
            commander.usage();
            return;
        }

        String text = null;
        if(m_commandLineArguments.m_inputUrl != null) {
            // retrieve URL and put into file
            try {
                text = Misc.retrieveUrl(URLEncoder.encode(m_commandLineArguments.m_inputUrl, "UTF-8"));
            } catch (IOException e) {
                System.err.println("URL could not be retrieved: " + e.getMessage());
                System.exit(2);
            }
        }
        else { // m_commandLineArguments.m_inputFile != null
            try {
                text = Misc.readFile(m_commandLineArguments.m_inputFile);
            } catch (IOException e) {
                System.err.println("File could not be read: " + e.getMessage());
                System.exit(2);
            }
        }

        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);

        ParseRunner<ObAstNode> parseRunner = null;
        if(m_commandLineArguments.m_parseRunner.equalsIgnoreCase("tracing")) {
            parseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
        }
        else if(m_commandLineArguments.m_parseRunner.equalsIgnoreCase("recovering")) {
            parseRunner = new RecoveringParseRunner<ObAstNode>(parser.Page());
        }
        else {
            parseRunner = new ReportingParseRunner<ObAstNode>(parser.Page());
        }

        ParsingResult<ObAstNode> result = parseRunner.run(text);

        if (result.hasErrors()) {
            System.out.println(ErrorUtils.printParseErrors(result));
            System.exit(1);
        } else {
            System.out.println("valid");
            System.exit(0);
        }
    }
}
