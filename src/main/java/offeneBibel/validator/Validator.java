package offeneBibel.validator;

import java.io.IOException;

import offeneBibel.osisExporter.EmptyVerseFixupVisitor;
import offeneBibel.osisExporter.WebsiteDbVisitor;
import offeneBibel.parser.AstNode;
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
    CommandLineArguments commandLineArguments;

    public static void main(String[] args) {
        Validator validator = new Validator();
        validator.run(args);
    }

    public void run(String[] args) {
    	try {
	        commandLineArguments = new CommandLineArguments();
	        JCommander commander = new JCommander(commandLineArguments, args);
	        if (commandLineArguments.help) {
	            commander.usage();
	            return;
	        }
	
	        if((commandLineArguments.inputFile != null && commandLineArguments.inputUrl != null) ||
	                                        (commandLineArguments.inputFile == null && commandLineArguments.inputUrl == null)) {
	            System.out.println("Specify either a file *or* a URL, not both.");
	            commander.usage();
	            return;
	        }
	
	        String text = null;
	        if(commandLineArguments.inputUrl != null) {
	            // retrieve URL and put into file
	            try {
	                text = Misc.retrieveUrl(commandLineArguments.inputUrl);
	            } catch (IOException e) {
	                System.err.println("URL could not be retrieved: " + e.getMessage());
	                System.exit(2);
	            }
	        }
	        else { // commandLineArguments.inputFile != null
	            try {
	                text = Misc.readFile(commandLineArguments.inputFile);
	            } catch (IOException e) {
	                System.err.println("File could not be read: " + e.getMessage());
	                System.exit(2);
	            }
	        }
	
	        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
	
	        ParseRunner<AstNode> parseRunner = null;
	        if(commandLineArguments.debugFile != null) {
	        	TracingParseRunnerListener<AstNode> listener
	        		= new TracingParseRunnerListener<>(commandLineArguments.debugFile);
	        	parseRunner = new EventBasedParseRunner<AstNode>(parser.Page());
	        	((EventBasedParseRunner<AstNode>)parseRunner).registerListener(listener);
	        }
	        else if(commandLineArguments.parseRunner.equalsIgnoreCase("tracing")) {
	            parseRunner = new TracingParseRunner<AstNode>(parser.Page());
	        }
	        else if(commandLineArguments.parseRunner.equalsIgnoreCase("recovering")) {
	            parseRunner = new RecoveringParseRunner<AstNode>(parser.Page(), commandLineArguments.timeout);
	        }
	        else {
	            parseRunner = new ReportingParseRunner<AstNode>(parser.Page());
	        }
	
	        ParsingResult<AstNode> result = parseRunner.run(text);
	
	        if (result.hasErrors()) {
	            System.out.println("invalid");
	            System.out.println(ErrorUtils.printParseErrors(result));
	            System.exit(1);
	        } else {
	        	if(commandLineArguments.json) {
		        	AstNode node = result.resultValue;
	                node.host(new EmptyVerseFixupVisitor());
	        		WebsiteDbVisitor visitor = new WebsiteDbVisitor();
	        		node.host(visitor);
	        		System.out.println("valid");
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
