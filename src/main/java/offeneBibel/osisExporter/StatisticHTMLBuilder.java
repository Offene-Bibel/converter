package offeneBibel.osisExporter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import offeneBibel.parser.ObVerseStatus;
import util.Misc;

public class StatisticHTMLBuilder {

    private static String[] COLORS = new String[] {
            "#0f0", "#5f0", "#af0", "#ff0", "#ff4",
            "#ff8", "#ffc", "#fff", "#f88", "#f00",
    };

    // for calling it manually; it is also called from exporter
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(new File(Misc.getResultsDir(), "offeneBibelStatus.properties"))) {
            props.load(in);
        }
        build(props);
    }

    public static void build(Properties props) throws Exception {
        Section gesamt = new Section("Gesamt");
        Section ot = new Section("AT"), nt = new Section("NT");
        gesamt.children.add(ot);
        gesamt.children.add(nt);
        parseProps(props, "AT", ot);
        parseProps(props, "NT", nt);
        gesamt.sum();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(Misc.getResultsDir(), "offeneBibelStatus.html"))))) {
            bw.write("<html>\n" +
                    "<head>\n" +
                    "<title>Offene Bibel Versstatistiken</title>\n" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
                    "<style type=\"text/css\">\n" +
                    "body { font-family: Verdana, Arial, Helvetica, sans-serif; }\n" +
                    "table {border-collapse: collapse; font-size: 6pt; }\n" +
                    "td, th {border: 1px solid #777; }\n" +
                    "</style>\n" +
                    "<script>\n" +
                    "function toggle(elem) {\n" +
                    "    var label = '';\n" +
                    "    var trs = document.getElementsByTagName('tr');\n" +
                    "    for (var i=0; i < trs.length; i++) {\n" +
                    "        var id = trs[i].id;\n" +
                    "        if (id == elem) {\n" +
                    "            label = trs[i].getElementsByTagName('a')[0].innerHTML;\n" +
                    "            if (label == '[+]')\n" +
                    "                trs[i].getElementsByTagName('a')[0].innerHTML = '[-]';\n" +
                    "            else\n" +
                    "                trs[i].getElementsByTagName('a')[0].innerHTML = '[+]';\n" +
                    "        } else if (id.length > elem.length && id.substring(0, elem.length+1) == elem+'_') {\n" +
                    "            if (label=='[-]') {\n" +
                    "                trs[i].style.display='none';\n" +
                    "            } else if (id.substring(elem.length+1).indexOf('_') == -1) {\n" +
                    "                trs[i].style.display='';\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n" +
                    "window.onload = function() {\n" +
                    "    document.getElementById('root').style.display='';\n" +
                    "    toggle('root');\n" +
                    "}\n" +
                    "</script>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<h1>Offene Bibel Versstatistiken</h1>\n" +
                    "<p>Stand: " + props.getProperty("DATE") + "</p>\n" +
                    "<table>");
            for (int i = 0; i < COLORS.length; i++) {
                bw.write("<tr><th style=\"background-color: " + COLORS[i] + "\">#" + (9 - i) + "</th><td>");
                int statusCount = ObVerseStatus.values().length;
                if (i < statusCount) {
                    bw.write(ObVerseStatus.values()[statusCount - 1 - i].getHumanReadableString());
                } else if (i == statusCount) {
                    bw.write("Leer");
                } else if (i == statusCount + 1) {
                    bw.write("Fehlt");
                }
                bw.write("</td></tr>");
            }
            bw.write("</table>\n<br />\n");
            bw.write("<table><tr><th>Abschnitt</th>");
            for (String fassung : new String[] { "SF", "LF" }) {
                bw.write("<th>" + fassung + " Graph</th>");
                for (int i = 0; i < 10; i++) {
                    bw.write("<th style=\"background-color: " + COLORS[i] + "\">" + fassung + " #" + (9 - i) + "</th>");
                }
            }
            bw.write("</tr>\n");
            appendHTML(bw, 0, "root", gesamt);
            bw.write("</table>");
        }
    }

    private static void parseProps(Properties props, String key, Section parent) {
        for (String book : props.getProperty(key).split(",")) {
            int chapCount = Integer.parseInt(props.getProperty(book));
            if (chapCount == 1) {
                parent.children.add(new Section(book, props.getProperty(book + ",0,LF"), props.getProperty(book + ",0,SF")));
            } else {
                Section bookSection = new Section(book);
                parent.children.add(bookSection);
                for (int i = 1; i <= chapCount; i++) {
                    bookSection.children.add(new Section(book + " " + i, props.getProperty(book + "," + i + ",LF"), props.getProperty(book + "," + i + ",SF")));
                }
            }
        }
    }

    private static void appendHTML(BufferedWriter bw, int depth, String path, Section section) throws Exception {
        bw.write("<tr id=\""+path+"\" style=\"display:none\"><td><span style=\"display:block; float:left; width:" + depth + "em\">&nbsp;</span><a href=\"javascript:toggle('"+path+"');\">[+]</a> ");

        if (depth > 1) {
            bw.write("<a href=\"http://www.offene-bibel.de/wiki/?title=" + section.name.replace(' ', '_') + "\">" + section.name + "</a>");
        } else {
            bw.write(section.name);
        }
        bw.write("</td>");
        for (int[] scores : new int[][] { section.scoresSF, section.scoresLF }) {
            int sum = 0, sum2 = 0, pxsum = 0;
            for (int i = 0; i < scores.length; i++) {
                sum += scores[i];
            }
            if (sum == 0)
                sum = 1;
            bw.write("<td><div style=\"width: 250px; position:relative; border: 1px solid black\">");
            for (int i = 0; i < scores.length; i++) {
                sum2 += scores[i];
                int px = (250 * sum2 / sum) - pxsum;
                pxsum += px;
                bw.write("<div style=\"float:left; width:" + px + "px; background-color: " + COLORS[i] + "\">&nbsp;</div>");
            }
            bw.write("</div></td>");
            for (int i = 0; i < scores.length; i++) {
                bw.write("<td>" + scores[i] + " " + "</td>");
            }
        }
        bw.write("</tr>\n");
        int ctr = 1;
        for (Section child : section.children) {
            appendHTML(bw, depth + 1, path+"_"+ctr, child);
            ctr++;
        }
    }

    private static class Section {
        public final String name;
        public final int[] scoresLF = new int[10], scoresSF = new int[10];
        public List<Section> children = new ArrayList<Section>();

        public Section(String name) {
            this.name = name.replace('_', ' ');
        }

        public Section(String name, String scoreLFText, String scoreSFText) {
            this(name);
            String[] scoreLFParts = scoreLFText.split(",");
            if (scoreLFParts.length != 10)
                throw new IllegalArgumentException(scoreLFText);
            for (int i = 0; i < scoreLFParts.length; i++) {
                scoresLF[i] = Integer.parseInt(scoreLFParts[i]);
            }
            String[] scoreSFParts = scoreSFText.split(",");
            if (scoreSFParts.length != 10)
                throw new IllegalArgumentException(scoreSFText);
            for (int i = 0; i < scoreSFParts.length; i++) {
                scoresSF[i] = Integer.parseInt(scoreSFParts[i]);
            }
        }

        public void sum() {
            for (Section child : children) {
                child.sum();
                for (int i = 0; i < scoresLF.length; i++) {
                    scoresLF[i] += child.scoresLF[i];
                }
                for (int i = 0; i < scoresSF.length; i++) {
                    scoresSF[i] += child.scoresSF[i];
                }
            }
        }
    }
}
