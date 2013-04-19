package offeneBibel.osisExporter;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class CommandLineArguments {
	@Parameter(names = { "-e", "-exportLevel" }, description = "Required translation status for export, 0=all,7=all criteria met", validateWith=ValidateLevel.class)
	int m_exportLevel = 0;
	public class ValidateLevel implements IParameterValidator {
		public void validate(String name, String value) throws ParameterException {
			int level = Integer.parseInt(value);
			if (level < 0 || level > 7) {
				throw new ParameterException("Parameter " + name + " should be between \"0\" and \"7\".");
			}
		}
	}

	@Parameter(names = { "-r", "-runner" }, description = "Runner to use for error reporting. One of: \"reporting\", \"tracing\" or \"recovering\"", validateWith=ValidateRunner.class)
	String m_parseRunner = "reporting";
	public class ValidateRunner implements IParameterValidator {
		public void validate(String name, String value) throws ParameterException {
			if (!value.equalsIgnoreCase("reporting") && !value.equalsIgnoreCase("tracing") && !value.equalsIgnoreCase("recovering")) {
				throw new ParameterException("Parameter " + name + " should either be \"reporting\", \"tracing\" or \"recovering\".");
			}
		}
	}
	
	@Parameter(names = { "-R", "-reloadOnError" }, description = "If parsing fails for a page, redownload it and retry.")
	boolean m_reloadOnError = false;
}
