package com.silvanaya.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.silvanaya.dto.ReportData;
import com.silvanaya.dto.ReportData.Col;
import com.silvanaya.dto.ReportData.Stat;
import com.silvanaya.model.CompanySettings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;


@Service
public class ReportExportService {

    /* ============================ EXCEL ============================ */
    public byte[] excel(ReportData d, CompanySettings s) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sh = wb.createSheet(sheetName(d.title()));
            XSSFColor accent = new XSSFColor(rgb(s.getAccentColor()), null);
            XSSFColor line = new XSSFColor(new byte[]{(byte) 226, (byte) 232, (byte) 240}, null);

            XSSFFont big = wb.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 15);
            XSSFFont bold = wb.createFont();
            bold.setBold(true);
            XSSFFont white = wb.createFont();
            white.setBold(true);
            white.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
            XSSFFont grey = wb.createFont();
            grey.setColor(new XSSFColor(new byte[]{(byte) 100, (byte) 116, (byte) 139}, null));

            DataFormat df = wb.createDataFormat();
            XSSFCellStyle titleSt = wb.createCellStyle();
            titleSt.setFont(big);
            XSSFCellStyle boldSt = wb.createCellStyle();
            boldSt.setFont(bold);
            XSSFCellStyle greySt = wb.createCellStyle();
            greySt.setFont(grey);

            XSSFCellStyle head = wb.createCellStyle();
            head.setFont(white);
            head.setFillForegroundColor(accent);
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setAlignment(HorizontalAlignment.CENTER);
            border(head, line);

            XSSFCellStyle text = wb.createCellStyle();
            border(text, line);
            XSSFCellStyle center = wb.createCellStyle();
            center.setAlignment(HorizontalAlignment.CENTER);
            border(center, line);
            XSSFCellStyle money = wb.createCellStyle();
            money.setDataFormat(df.getFormat("#,##0.00"));
            money.setAlignment(HorizontalAlignment.RIGHT);
            border(money, line);
            XSSFCellStyle number = wb.createCellStyle();
            number.setDataFormat(df.getFormat("#,##0.##"));
            number.setAlignment(HorizontalAlignment.RIGHT);
            border(number, line);

            int r = 0;
            put(sh.createRow(r++), 0, s.getCompanyName(), titleSt);
            String contact = join(s.getAddress(), s.getPhone(), s.getEmail());
            if (!contact.isEmpty()) put(sh.createRow(r++), 0, contact, greySt);
            r++;
            put(sh.createRow(r++), 0, d.title(), boldSt);
            put(sh.createRow(r++), 0, d.period(), greySt);
            put(sh.createRow(r++), 0, "Generated " + d.generatedAt() + " by " + d.generatedBy(), greySt);
            r++;

            for (Stat st : d.summary()) {
                Row row = sh.createRow(r++);
                put(row, 0, st.label(), boldSt);
                put(row, 1, st.value(), null);
            }
            if (!d.summary().isEmpty()) r++;

            List<Col> cols = d.columns();
            int n = cols.size();
            int[] width = new int[n];

            int headerRow = r;
            Row hr = sh.createRow(r++);
            for (int j = 0; j < n; j++) {
                put(hr, j, cols.get(j).label(), head);
                width[j] = cols.get(j).label().length();
            }

            for (List<Object> data : d.rows()) {
                Row row = sh.createRow(r++);
                for (int j = 0; j < n; j++) {
                    Col col = cols.get(j);
                    Object v = j < data.size() ? data.get(j) : null;
                    Cell cell = row.createCell(j);
                    if (v instanceof Number num && ("money".equals(col.type()) || "number".equals(col.type()))) {
                        cell.setCellValue(num.doubleValue());
                        cell.setCellStyle("money".equals(col.type()) ? money : number);
                        width[j] = Math.max(width[j], 14);
                    } else {
                        String str = v == null ? "" : v.toString();
                        cell.setCellValue(str);
                        cell.setCellStyle("center".equals(col.align()) ? center : text);
                        width[j] = Math.max(width[j], str.length());
                    }
                }
            }
            if (d.rows().isEmpty()) put(sh.createRow(r), 0, "No data for this period", greySt);

            for (int j = 0; j < n; j++) sh.setColumnWidth(j, Math.min(48, Math.max(12, width[j] + 3)) * 256);
            sh.createFreezePane(0, headerRow + 1);
            sh.getPrintSetup().setLandscape(true);
            sh.setFitToPage(true);
            sh.getPrintSetup().setFitWidth((short) 1);
            sh.getPrintSetup().setFitHeight((short) 0);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void border(XSSFCellStyle st, XSSFColor color) {
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        st.setTopBorderColor(color);
        st.setBottomBorderColor(color);
        st.setLeftBorderColor(color);
        st.setRightBorderColor(color);
    }

    private static void put(Row row, int col, String value, XSSFCellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        if (style != null) c.setCellStyle(style);
    }

    private static String sheetName(String t) {
        String n = (t == null ? "Report" : t).replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        return n.length() > 31 ? n.substring(0, 31) : (n.isEmpty() ? "Report" : n);
    }

    /* ============================ PDF ============================ */
    public byte[] pdf(ReportData d, CompanySettings s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Col> cols = d.columns();
        boolean wide = cols.size() > 6;
        Color accent = color(s.getAccentColor());
        Color grey = new Color(100, 116, 139);

        Font nameF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font smallF = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, grey);
        Font titleF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.NORMAL, accent);
        Font headF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.NORMAL, Color.WHITE);
        Font cellF = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font boldF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

        Document doc = new Document(wide ? PageSize.A4.rotate() : PageSize.A4, 28, 28, 28, 44);
        try {
            PdfWriter w = PdfWriter.getInstance(doc, out);
            w.setPageEvent(new Footer(s.getCompanyName() + "  |  " + d.title(), smallF));
            doc.open();

            /* Kichwa: nembo + kampuni (kushoto), jina la ripoti (kulia) */
            PdfPTable head = new PdfPTable(new float[]{3f, 2f});
            head.setWidthPercentage(100);
            PdfPCell left = new PdfPCell();
            left.setBorder(Rectangle.NO_BORDER);
            Image logo = logo(s);
            if (logo != null && s.isInvoiceShowLogo()) {
                logo.scaleToFit(80, 50);
                left.addElement(logo);
            }
            left.addElement(new Paragraph(s.getCompanyName(), nameF));
            String contact = join(s.getAddress(), s.getPhone(), s.getEmail());
            if (!contact.isEmpty()) left.addElement(new Paragraph(contact, smallF));
            PdfPCell right = new PdfPCell();
            right.setBorder(Rectangle.NO_BORDER);
            Paragraph t = new Paragraph(d.title(), titleF);
            t.setAlignment(Element.ALIGN_RIGHT);
            Paragraph p1 = new Paragraph(d.period(), smallF);
            p1.setAlignment(Element.ALIGN_RIGHT);
            Paragraph p2 = new Paragraph("Generated " + d.generatedAt() + " by " + d.generatedBy(), smallF);
            p2.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(t);
            right.addElement(p1);
            right.addElement(p2);
            head.addCell(left);
            head.addCell(right);
            doc.add(head);
            doc.add(new LineSeparator(2f, 100f, accent, Element.ALIGN_CENTER, -4));
            doc.add(new Paragraph(" ", smallF));

            /* Muhtasari */
            if (!d.summary().isEmpty()) {
                PdfPTable sum = new PdfPTable(new float[]{2.2f, 2f, 2.2f, 2f});
                sum.setWidthPercentage(100);
                List<Stat> stats = d.summary();
                for (int i = 0; i < stats.size(); i++) {
                    sum.addCell(statCell(stats.get(i).label(), smallF, new Color(248, 250, 252)));
                    sum.addCell(statCell(stats.get(i).value(), boldF, new Color(248, 250, 252)));
                }
                if (stats.size() % 2 == 1) {
                    sum.addCell(statCell("", smallF, new Color(248, 250, 252)));
                    sum.addCell(statCell("", boldF, new Color(248, 250, 252)));
                }
                doc.add(sum);
                doc.add(new Paragraph(" ", smallF));
            }

            /* Jedwali */
            int n = cols.size();
            float[] weights = new float[n];
            for (int j = 0; j < n; j++) weights[j] = Math.max(6, Math.min(26, cols.get(j).label().length()));
            for (List<Object> row : d.rows())
                for (int j = 0; j < n && j < row.size(); j++)
                    weights[j] = Math.max(weights[j], Math.min(26, fmt(row.get(j), cols.get(j).type()).length()));

            PdfPTable table = new PdfPTable(n);
            table.setWidthPercentage(100);
            table.setWidths(weights);
            table.setHeaderRows(1);
            for (Col c : cols) {
                PdfPCell h = new PdfPCell(new Phrase(c.label(), headF));
                h.setBackgroundColor(accent);
                h.setPadding(5);
                h.setBorderColor(accent);
                h.setHorizontalAlignment(alignOf(c.align()));
                table.addCell(h);
            }
            int idx = 0;
            for (List<Object> row : d.rows()) {
                Color bg = idx++ % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);
                for (int j = 0; j < n; j++) {
                    Col c = cols.get(j);
                    PdfPCell cell = new PdfPCell(new Phrase(fmt(j < row.size() ? row.get(j) : null, c.type()), cellF));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(4);
                    cell.setBorderColor(new Color(226, 232, 240));
                    cell.setHorizontalAlignment(alignOf(c.align()));
                    table.addCell(cell);
                }
            }
            if (d.rows().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No data for this period", smallF));
                empty.setColspan(n);
                empty.setPadding(18);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(empty);
            }
            doc.add(table);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF", e);
        }
        return out.toByteArray();
    }

    private static PdfPCell statCell(String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setBorderColor(new Color(226, 232, 240));
        c.setPadding(5);
        return c;
    }

    private static int alignOf(String a) {
        return "right".equals(a) ? Element.ALIGN_RIGHT : "center".equals(a) ? Element.ALIGN_CENTER : Element.ALIGN_LEFT;
    }

    private static String fmt(Object v, String type) {
        if (v == null) return "";
        if (v instanceof Number n) {
            if ("money".equals(type)) return String.format(Locale.US, "%,.2f", n.doubleValue());
            if ("number".equals(type)) {
                double x = n.doubleValue();
                return x == Math.rint(x) ? String.format(Locale.US, "%,d", (long) x) : String.format(Locale.US, "%,.2f", x);
            }
            return n.toString();
        }
        return v.toString();
    }

    /** Nembo kwa PDF: PNG au JPEG tu (SVG na WEBP zinarukwa). */
    private static Image logo(CompanySettings s) {
        try {
            String data = s.getLogoData();
            if (data == null || !(data.startsWith("data:image/png") || data.startsWith("data:image/jpeg")
                    || data.startsWith("data:image/jpg"))) return null;
            int comma = data.indexOf(',');
            if (comma < 0) return null;
            return Image.getInstance(Base64.getDecoder().decode(data.substring(comma + 1)));
        } catch (Exception e) {
            return null;
        }
    }

    private static class Footer extends PdfPageEventHelper {
        private final String text;
        private final Font font;

        Footer(String text, Font font) {
            this.text = text;
            this.font = font;
        }

        @Override
        public void onEndPage(PdfWriter w, Document doc) {
            PdfContentByte cb = w.getDirectContent();
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(text + "  |  Page " + w.getPageNumber(), font),
                    (doc.right() + doc.left()) / 2, doc.bottom() - 20, 0);
        }
    }

    /* ============================ Vya pamoja ============================ */
    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append("  |  ");
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static byte[] rgb(String hex) {
        Color c = color(hex);
        return new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()};
    }

    private static Color color(String hex) {
        try {
            return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16),
                    Integer.parseInt(hex.substring(5, 7), 16));
        } catch (Exception e) {
            return new Color(11, 95, 255);
        }
    }
}