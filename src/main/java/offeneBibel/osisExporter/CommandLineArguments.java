package offeneBibel.osisExporter;

import util.ValidateLevel;
import util.ValidateRunner;

import com.beust.jcommander.Parameter;

class CommandLineArguments {
	@Parameter(names = { "-e", "--exportLevel" }, description = "Required translation status for export, 0=no restrictions on criteria, 7=all criteria met", validateWith=ValidateLevel.class)
	int m_exportLevel = 0;
	
	@Parameter(names = { "-r", "--runner" }, description = "Runner to use for error reporting. One of: \"reporting\", \"tracing\" or \"recovering\"", validateWith=ValidateRunner.class)
	String m_parseRunner = "reporting";
	@Parameter(names = { "-R", "--reloadOnError" }, description = "If parsing fails for a page, redownload it and retry.")
	boolean m_reloadOnError = false;
	
	@Parameter(names = { "-c", "--continueOnError" }, description = "If an error occurs, skip that chapter and continue.")
	boolean m_continueOnError = false;
	
	@Parameter(names = {"-h", "--help"})
	boolean m_help;
}
