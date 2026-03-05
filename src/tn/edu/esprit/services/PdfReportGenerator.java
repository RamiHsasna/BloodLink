package tn.edu.esprit.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Zero-dependency PDF report generator.
 * Writes raw PDF 1.4 format directly to a file.
 */
public class PdfReportGenerator {

    private final List<byte[]> pages = new ArrayList<>();
    private final List<Long> objectOffsets = new ArrayList<>();
    private int objectCount = 0;
    private ByteArrayOutputStream buffer;

    // Page dimensions (A4 in points: 595 x 842)
    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 842f;
    private static final float MARGIN = 50f;

    /**
     * Genere un rapport PDF complet d'audit BloodLink.
     */
    public void generateReport(File outputFile,
            List<String[]> donationRows,
            List<String[]> transferRows,
            List<String[]> alertRows,
            int resolvedCount, int criticalCount) throws IOException {

        buffer = new ByteArrayOutputStream();

        // ========== Build Page 1: Title + Summary + Donation Logs ==========
        StringBuilder p1 = new StringBuilder();
        float y = PAGE_H - MARGIN;

        // Title
        y = drawText(p1, "BloodLink  -  Rapport d'audit", MARGIN, y, 20, "0.118 0.161 0.231");
        y -= 8;
        y = drawText(p1, "Genere le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm")),
                MARGIN, y, 10, "0.4 0.46 0.53");
        y -= 5;

        // Separator line
        p1.append(String.format("%.1f %.1f m %.1f %.1f l S\n", MARGIN, y, PAGE_W - MARGIN, y));
        y -= 20;

        // Summary box
        y = drawText(p1, "RESUME", MARGIN, y, 14, "0.2 0.26 0.33");
        y -= 6;
        y = drawText(p1, "Total journaux de dons :       " + donationRows.size(), MARGIN + 10, y, 10, "0.28 0.33 0.41");
        y = drawText(p1, "Total journaux de transferts : " + transferRows.size(), MARGIN + 10, y, 10, "0.28 0.33 0.41");
        y = drawText(p1, "Total alertes d'urgence :      " + alertRows.size(), MARGIN + 10, y, 10, "0.28 0.33 0.41");
        y = drawText(p1, "Alertes resolues :             " + resolvedCount + " / " + alertRows.size(), MARGIN + 10, y,
                10, "0.09 0.64 0.38");
        y = drawText(p1, "Alertes critiques :               " + criticalCount, MARGIN + 10, y, 10, "0.86 0.15 0.15");
        y -= 20;

        // Donation Logs Table
        y = drawText(p1, "JOURNAUX DE DONS", MARGIN, y, 13, "0.2 0.26 0.33");
        y -= 6;

        // Table header
        String[] donHeaders = { "ID Journal", "ID Don", "Action", "Statut Prec.", "Nouveau Statut" };
        float[] donWidths = { 70, 70, 90, 110, 110 };
        y = drawTableHeader(p1, donHeaders, donWidths, MARGIN, y);

        int maxDon = Math.min(donationRows.size(), 20);
        for (int i = 0; i < maxDon; i++) {
            y = drawTableRow(p1, donationRows.get(i), donWidths, MARGIN, y, i % 2 == 0);
            if (y < MARGIN + 30)
                break;
        }

        pages.add(buildPageStream(p1.toString()));

        // ========== Build Page 2: Transfer Logs + Alerts ==========
        StringBuilder p2 = new StringBuilder();
        y = PAGE_H - MARGIN;

        y = drawText(p2, "JOURNAUX DE TRANSFERTS", MARGIN, y, 13, "0.2 0.26 0.33");
        y -= 6;

        String[] trHeaders = { "ID Journal", "ID Transfert", "Action", "Statut Prec.", "Nouveau Statut" };
        float[] trWidths = { 70, 70, 90, 110, 110 };
        y = drawTableHeader(p2, trHeaders, trWidths, MARGIN, y);

        int maxTr = Math.min(transferRows.size(), 15);
        for (int i = 0; i < maxTr; i++) {
            y = drawTableRow(p2, transferRows.get(i), trWidths, MARGIN, y, i % 2 == 0);
            if (y < PAGE_H / 2)
                break;
        }

        y -= 25;

        // Alerts section
        y = drawText(p2, "ALERTES D'URGENCE", MARGIN, y, 13, "0.93 0.27 0.27");
        y -= 6;

        String[] alHeaders = { "Titre", "Severite", "Groupe", "Qte", "Resolue" };
        float[] alWidths = { 160, 80, 70, 50, 70 };
        y = drawTableHeader(p2, alHeaders, alWidths, MARGIN, y);

        int maxAl = Math.min(alertRows.size(), 15);
        for (int i = 0; i < maxAl; i++) {
            y = drawTableRow(p2, alertRows.get(i), alWidths, MARGIN, y, i % 2 == 0);
            if (y < MARGIN + 30)
                break;
        }

        // Footer
        y = MARGIN;
        drawText(p2, "Module BloodLink  |  Confidentiel  |  Page 2", MARGIN, y, 8, "0.6 0.6 0.6");

        pages.add(buildPageStream(p2.toString()));

        // ========== Write the PDF file ==========
        writePdfFile(outputFile);
    }

    // ==================== PDF Drawing Helpers ====================

    private float drawText(StringBuilder stream, String text, float x, float y, int fontSize, String rgbColor) {
        text = escapePdf(text);
        stream.append("BT\n");
        stream.append(String.format("%s rg\n", rgbColor));
        stream.append(String.format("/F1 %d Tf\n", fontSize));
        stream.append(String.format("%.1f %.1f Td\n", x, y));
        stream.append(String.format("(%s) Tj\n", text));
        stream.append("ET\n");
        return y - fontSize - 4;
    }

    private float drawTableHeader(StringBuilder stream, String[] headers, float[] widths, float x, float y) {
        // Header background
        float totalW = 0;
        for (float w : widths)
            totalW += w;
        stream.append(String.format("0.93 0.95 0.97 rg\n"));
        stream.append(String.format("%.1f %.1f %.1f %.1f re f\n", x, y - 14f, totalW, 16f));

        // Header text
        float cx = x + 4;
        for (int i = 0; i < headers.length; i++) {
            stream.append("BT\n");
            stream.append("0.28 0.33 0.41 rg\n");
            stream.append(String.format("/F1 8 Tf\n"));
            stream.append(String.format("%.1f %.1f Td\n", cx, y - 11f));
            stream.append(String.format("(%s) Tj\n", escapePdf(headers[i])));
            stream.append("ET\n");
            cx += widths[i];
        }
        return y - 18;
    }

    private float drawTableRow(StringBuilder stream, String[] cols, float[] widths, float x, float y, boolean shaded) {
        float totalW = 0;
        for (float w : widths)
            totalW += w;

        if (shaded) {
            stream.append("0.97 0.98 0.99 rg\n");
            stream.append(String.format("%.1f %.1f %.1f %.1f re f\n", x, y - 12f, totalW, 14f));
        }

        float cx = x + 4;
        for (int i = 0; i < Math.min(cols.length, widths.length); i++) {
            String val = cols[i] != null ? cols[i] : "";
            // Truncate to fit column
            int maxChars = (int) (widths[i] / 5.5);
            if (val.length() > maxChars)
                val = val.substring(0, maxChars) + "...";

            stream.append("BT\n");
            stream.append("0.12 0.16 0.23 rg\n");
            stream.append(String.format("/F1 8 Tf\n"));
            stream.append(String.format("%.1f %.1f Td\n", cx, y - 9f));
            stream.append(String.format("(%s) Tj\n", escapePdf(val)));
            stream.append("ET\n");
            cx += widths[i];
        }

        // Row border line
        stream.append("0.88 0.91 0.94 RG\n");
        stream.append(String.format("%.1f %.1f m %.1f %.1f l S\n", x, y - 13f, x + totalW, y - 13f));

        return y - 15;
    }

    private String escapePdf(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\n", " ")
                .replace("\r", "");
    }

    // ==================== PDF Structure ====================

    private byte[] buildPageStream(String content) {
        return content.getBytes(StandardCharsets.ISO_8859_1);
    }

    private void writePdfFile(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Header
            write(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

            // Object 1: Catalog
            objectOffsets.add((long) out.size());
            write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // Object 2: Pages (parent)
            objectOffsets.add((long) out.size());
            StringBuilder kidsStr = new StringBuilder();
            for (int i = 0; i < pages.size(); i++) {
                if (i > 0)
                    kidsStr.append(" ");
                kidsStr.append((4 + i * 2) + " 0 R");
            }
            write(out, String.format(
                    "2 0 obj\n<< /Type /Pages /Kids [%s] /Count %d >>\nendobj\n",
                    kidsStr.toString(), pages.size()));

            // Object 3: Font (Helvetica)
            objectOffsets.add((long) out.size());
            write(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            int nextObj = 4;

            // For each page: page object + content stream
            for (int p = 0; p < pages.size(); p++) {
                byte[] streamData = pages.get(p);

                // Page object
                objectOffsets.add((long) out.size());
                int pageObjNum = nextObj;
                int contentObjNum = nextObj + 1;
                write(out, String.format(
                        "%d 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.0f %.0f] " +
                                "/Contents %d 0 R /Resources << /Font << /F1 3 0 R >> >> >>\nendobj\n",
                        pageObjNum, PAGE_W, PAGE_H, contentObjNum));
                nextObj++;

                // Content stream
                objectOffsets.add((long) out.size());
                write(out, String.format(
                        "%d 0 obj\n<< /Length %d >>\nstream\n",
                        contentObjNum, streamData.length));
                out.write(streamData);
                write(out, "\nendstream\nendobj\n");
                nextObj++;
            }

            // Cross-reference table
            long xrefOffset = out.size();
            int totalObjects = objectOffsets.size() + 1; // +1 for free object entry
            write(out, String.format("xref\n0 %d\n", totalObjects));
            write(out, "0000000000 65535 f \n");
            for (long offset : objectOffsets) {
                write(out, String.format("%010d 00000 n \n", offset));
            }

            // Trailer
            write(out, String.format(
                    "trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n",
                    totalObjects, xrefOffset));

            raf.write(out.toByteArray());
        }
    }

    private void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
