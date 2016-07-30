package parser;

import org.testng.annotations.Test;

import static parser.ParseTester.*;

public class BasicTest {
    @Test
    public void basicPageWorks() {
        parseOk( "{{Lesefassung}}\n\n"
                + "''(kommt später)''\n\n"
                + "{{Studienfassung}}\n\n"
                + "{{S|1}}"
                + "{{Bemerkungen}}\n\n"
                + "{{Kapitelseite Fuß}}\n" );
    }

    @Test
    public void basicPageFails() {
        parseFails( "{{LeFXung}}\n\n"
                + "''(kommt später)''\n\n"
                + "{{Studienfassung}}\n\n"
                + "{{S|1}}"
                + "{{Bemerkungen}}\n\n"
                + "{{Kapitelseite Fuß}}\n" );
    }
}

