package offeneBibel.osisExporter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class Testing
{

	public static void main(String[] args)
	{
		String text;
		try {
			text = readFile("resources/genesis1.txt");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
		BasicParseRunner<ObAstNode> parseRunner = new BasicParseRunner<ObAstNode>(parser.Page());
		ParsingResult<ObAstNode> result = parseRunner.run(text);

		if(true || result.hasErrors()) {
			TracingParseRunner<ObAstNode> tracingParseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
			ParsingResult<ObAstNode> tracingResult = parseRunner.run(text);
			
			System.out.println("Tree:");
			String parseTreePrintOut = ParseTreeUtils.printNodeTree(tracingResult);
			System.out.println(parseTreePrintOut);
			
			System.out.println("Errors:");
			ErrorUtils.printParseErrors(parseRunner.getParseErrors());
			
			System.out.println("Log:");
			System.out.println(tracingParseRunner.getLog());
		}
		else {
			ObAstVisitor visitor = new ObAstVisitor(23, "Ps");
			ObAstNode node = result.resultValue;
			try {
				node.host(visitor);
			} catch (Throwable e) {
				e.printStackTrace();
				return;
			}
			
			System.out.println("===========================================");
			System.out.println("Studienfassung:");
			System.out.println(visitor.getStudienFassung());
			System.out.println("===========================================");
			System.out.println("Lesefassung:");
			System.out.println(visitor.getLeseFassung());
		}
	}

	private static String readFile(String file) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		return readBufferToString(reader);
	}
	
	private static String readBufferToString(BufferedReader reader) throws IOException
	{
		String result = "";
		char[] cbuf = new char[512];
		int readCount;
		while((readCount = reader.read(cbuf)) >= 0)
			result += String.valueOf(cbuf, 0, readCount);
		return result;
	}
}
