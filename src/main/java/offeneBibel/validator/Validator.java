package offeneBibel.validator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

public class Validator
{

	public static void main(String[] args)
	{
		if(args.length != 1) {
			System.out.println("Invalid number of arguments.");
			System.exit(3);
			return;
		}
		String text;
		try {
			text = readFile(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
			return;
		}

		OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
		ReportingParseRunner<ObAstNode> parseRunner = new ReportingParseRunner<ObAstNode>(parser.Page());
		//RecoveringParseRunner<ObAstNode> parseRunner = new RecoveringParseRunner<ObAstNode>(parser.Page());
		ParsingResult<ObAstNode> result = parseRunner.run(text);

		if(result.hasErrors()) {
			System.out.println(ErrorUtils.printParseErrors(result));
			System.exit(1);
		}
		else {
			System.out.println("success");
			System.exit(0);
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
