package offeneBibel.validator;

import util.ValidateRunner;

import com.beust.jcommander.Parameter;

class CommandLineArguments {
    @Parameter(names = { "-r", "--runner" }, description = "Runner to use for error reporting. One of: \"reporting\", \"tracing\" " +
                                    "or \"recovering\"", validateWith=ValidateRunner.class)
    String m_parseRunner = "reporting";

    @Parameter(names = { "-d", "--debugFile" }, description = "Where to save the debug file.")
    String m_debugFile = null;

    @Parameter(names = { "-i", "--inputFile" }, description = "File to validate.")
    String m_inputFile = null;

    @Parameter(names = { "-u", "--url" }, description = "URL to download content from to validate.")
    String m_inputUrl = null;

    @Parameter(names = { "-t", "--timeout" }, description = "Number of milliseconds to wait for the parser before aborting.")
    long m_timeout = 10000;

    @Parameter(names = {"-h", "--help"})
    boolean m_help;
}
