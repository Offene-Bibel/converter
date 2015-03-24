package offeneBibel.validator;

import java.io.IOException;

import offeneBibel.osisExporter.ObWebsiteDbVisitor;
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
import com.github.parboiled1.grappa.backport.EventBasedParseRunner;
import com.github.parboiled1.grappa.backport.TracingParseRunnerListener;

public class Validator {
    CommandLineArguments m_commandLineArguments;

    public static void main(String[] args) {
        Validator validator = new Validator();
        validator.run(args);
    }

    public void run(String[] args) {
    	try {
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
	                text = Misc.retrieveUrl(m_commandLineArguments.m_inputUrl);
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
	        if(m_commandLineArguments.m_debugFile != null) {
	        	TracingParseRunnerListener<ObAstNode> listener
	        		= new TracingParseRunnerListener<>(m_commandLineArguments.m_debugFile);
	        	parseRunner = new EventBasedParseRunner<ObAstNode>(parser.Page());
	        	((EventBasedParseRunner<ObAstNode>)parseRunner).registerListener(listener);
	        }
	        else if(m_commandLineArguments.m_parseRunner.equalsIgnoreCase("tracing")) {
	            parseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
	        }
	        else if(m_commandLineArguments.m_parseRunner.equalsIgnoreCase("recovering")) {
	            parseRunner = new RecoveringParseRunner<ObAstNode>(parser.Page(), m_commandLineArguments.m_timeout);
	        }
	        else {
	            parseRunner = new ReportingParseRunner<ObAstNode>(parser.Page());
	        }
	
	        ParsingResult<ObAstNode> result = parseRunner.run(text);
	
	        if (result.hasErrors()) {
	            System.out.println("invalid");
	            System.out.println(ErrorUtils.printParseErrors(result));
	            System.exit(1);
	        } else {
	        	if(m_commandLineArguments.m_json) {
	        		ObWebsiteDbVisitor visitor = new ObWebsiteDbVisitor();
					result.resultValue.host(visitor);
	                System.out.println(visitor.getResult());
	        	}
	        	else {
	        		System.out.println("valid");
	        	}
	            System.exit(0);
	        }
    	} catch (Throwable e) {
    		e.printStackTrace();
    		System.exit(2);
    	}
    }
}
