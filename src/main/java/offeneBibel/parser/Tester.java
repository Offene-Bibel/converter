package offeneBibel.parser;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.parboiled.Parboiled;
import org.parboiled.buffers.DefaultInputBuffer;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

class Tester
{
	public static void main(String [] args)
	{
		try {
			FileReader testInputReader = new FileReader("resources/psalm23.txt");
			BufferedReader reader = new BufferedReader(testInputReader);
			CharArrayWriter charWriter = new CharArrayWriter();
			char[] cbuf = new char[512];
			int readCount;
			try {
				while((readCount = reader.read(cbuf)) >= 0)
					charWriter.write(cbuf, 0, readCount);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
			TracingParseRunner<ObAstNode> parseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
			InputBuffer parboiledInputBuffer = new DefaultInputBuffer(charWriter.toCharArray());
			ParsingResult<ObAstNode> result = parseRunner.run(parboiledInputBuffer);
			String parseTreePrintOut = ParseTreeUtils.printNodeTree(result);
			System.out.println("Tree:");
			System.out.println(parseTreePrintOut);
			System.out.println("Errors:");
			ErrorUtils.printParseErrors(parseRunner.getParseErrors());
			System.out.println("Log:");
			System.out.println(parseRunner.getLog());
			System.out.println("End");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}