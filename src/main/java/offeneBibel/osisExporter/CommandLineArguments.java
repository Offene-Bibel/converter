package offeneBibel.osisExporter;

import com.beust.jcommander.Parameter;

public class CommandLineArguments {
	@Parameter(names = { "-e", "-exportLevel" }, description = "Required translation status for export, 0=all,7=all criteria met")
	int m_exportLevel = 0;
}
