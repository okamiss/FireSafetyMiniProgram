package com.firesafety.platform.training;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfWriter;

public class OpenPdfCertificateRenderer implements CertificateRenderer {
    private static final String FONT_RESOURCE = "/fonts/NotoSansCJKsc-Regular.otf";

    @Override
    public byte[] render(CertificateContent content) {
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4.rotate(), 72, 72, 64, 64);
            var writer = PdfWriter.getInstance(document, output);
            document.open();
            drawBorder(writer);

            var baseFont = loadFont();
            document.add(paragraph("消防安全培训证书", new Font(baseFont, 30, Font.BOLD, new Color(180, 35, 24)), 18));
            document.add(paragraph("CERTIFICATE OF COMPLETION", new Font(baseFont, 12, Font.NORMAL, Color.DARK_GRAY), 34));
            document.add(paragraph("兹证明", new Font(baseFont, 16), 16));
            document.add(paragraph(content.participantName(), new Font(baseFont, 24, Font.BOLD), 18));
            document.add(paragraph("已完成「" + content.taskTitle() + "」并通过考核", new Font(baseFont, 17), 14));
            document.add(paragraph("所属企业：" + content.enterpriseName(), new Font(baseFont, 14), 8));
            document.add(paragraph("通过日期：" + content.passedDate().format(DateTimeFormatter.ISO_LOCAL_DATE), new Font(baseFont, 14), 22));
            document.add(paragraph(content.issuerName(), new Font(baseFont, 13), 6));
            document.add(paragraph("证书编号：" + content.certificateNo(), new Font(baseFont, 11), 0));
            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Unable to render training certificate", exception);
        }
    }

    private BaseFont loadFont() throws IOException, DocumentException {
        try (var input = getClass().getResourceAsStream(FONT_RESOURCE)) {
            if (input == null) throw new IOException("Certificate font is missing");
            return BaseFont.createFont(
                    "NotoSansCJKsc-Regular.otf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, input.readAllBytes(), null);
        }
    }

    private Paragraph paragraph(String text, Font font, float spacingAfter) {
        var paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private void drawBorder(PdfWriter writer) {
        var canvas = writer.getDirectContent();
        canvas.setColorStroke(new Color(180, 35, 24));
        canvas.setLineWidth(2.5f);
        canvas.rectangle(34, 34, PageSize.A4.getHeight() - 68, PageSize.A4.getWidth() - 68);
        canvas.stroke();
    }
}
