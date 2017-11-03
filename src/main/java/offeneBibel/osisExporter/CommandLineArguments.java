package offeneBibel.osisExporter;

import util.ValidateBook;
import util.ValidateLevel;
import util.ValidateRunner;

import com.beust.jcommander.Parameter;

class CommandLineArguments {
    @Parameter(names = { "-e", "--exportLevel" }, description = "Required translation status for export, 0=unchecked, 3=very good", validateWith=ValidateLevel.class)
    int exportLevel = 0;

    @Parameter(names= { "-b", "--book"}, description="Comma separated list of books to export (empty to export all)", validateWith=ValidateBook.class)
    String books = "";

    @Parameter(names= { "-d", "--divineName"}, description="Desired divine name style (0-13)")
    int divineNameStyle = 0;

    @Parameter(names = { "-r", "--runner" }, description = "Runner to use for error reporting. One of: \"reporting\", \"tracing\" or \"recovering\"", validateWith=ValidateRunner.class)
    String parseRunner = "reporting";
    @Parameter(names = { "-R", "--reloadOnError" }, description = "If parsing fails for a page, redownload it and retry.")
    boolean reloadOnError = false;

    @Parameter(names = { "-c", "--continueOnError" }, description = "If an error occurs, skip that chapter and continue.")
    boolean continueOnError = false;

    @Parameter(names = { "-p", "--tryPreviousVersions" }, description = "If an error occurs, try to load a previous version of that chapter and retry.")
    boolean tryPreviousVersions = false;

    @Parameter(names = { "--skipGenerateOSIS" }, description = "Skip generation of OSIS documents.")
    boolean skipGenerateOsis = false;

    @Parameter(names = { "--generateWeb" }, description = "Generate the webviewer backing files.")
    boolean generateWeb = false;

    @Parameter(names = { "-s", "--generateStatistics" }, description = "Generate statistics of verse status")
    boolean generateStatistics = false;


    @Parameter(names = { "-a", "--cacheAST" }, description = "Cache the already parsed ASTs. Delete them manually if you edited the parser code.")
    boolean cacheAST = false;

    @Parameter(names = { "--saveRAM" }, description = "Persist the parsed ASTs to disk and only load them when needed. Use this if you run out of RAM running the parser.")
    boolean saveRAM = false;

    @Parameter(names = { "-i", "--inlineVersStatus" }, description = "Show verse status as a footnote at the beginning of the verse.")
    boolean inlineVerseStatus = false;

    @Parameter(names = { "-l", "--lineGroupUnmilestoned" }, description = "Create line group and line tags without milestone; create milestones for quote tags instead to make document well-formed.")
    boolean unmilestonedLineGroup = false;

    @Parameter(names = {"-h", "--help"})
    boolean help;
}
