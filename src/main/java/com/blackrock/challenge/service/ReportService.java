package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Service for generating PDF investment comparison reports.
 *
 * <p>Uses OpenPDF (LGPL) to produce a fully styled A4 PDF containing:
 * - Investor profile summary
 * - NPS returns table with tax benefit
 * - Index Fund returns table
 * - Side-by-side comparison and winner recommendation
 * - Period-by-period narrative summaries
 *
 * <p>Endpoint: POST /blackrock/challenge/v1/returns:report
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final CompareService              compareService;
    private final HybridAllocationService     hybridAllocationService;
    private final RetirementIncomeService     retirementIncomeService;
    private final AlternativeInvestmentService alternativeInvestmentService;

    // ─── Brand Colors ─────────────────────────────────────────────────────────
    private static final Color COLOR_DARK    = new Color(20, 20, 20);
    private static final Color COLOR_GOLD    = new Color(194, 148, 57);
    private static final Color COLOR_HEADER  = new Color(31, 31, 31);
    private static final Color COLOR_ALT_ROW = new Color(248, 248, 248);
    private static final Color COLOR_GREEN   = new Color(0, 120, 0);
    private static final Color COLOR_BLUE    = new Color(0, 60, 140);
    private static final Color COLOR_GRAY    = new Color(120, 120, 120);

    // ─── Fonts ────────────────────────────────────────────────────────────────
    private static Font titleFont()      { return new Font(Font.HELVETICA, 22, Font.BOLD, Color.WHITE); }
    private static Font subtitleFont()   { return new Font(Font.HELVETICA, 10, Font.NORMAL, Color.LIGHT_GRAY); }
    private static Font sectionFont()    { return new Font(Font.HELVETICA, 13, Font.BOLD, COLOR_DARK); }
    private static Font labelFont()      { return new Font(Font.HELVETICA, 9,  Font.BOLD, COLOR_GRAY); }
    private static Font valueFont()      { return new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_DARK); }
    private static Font normalFont()     { return new Font(Font.HELVETICA, 9,  Font.NORMAL, COLOR_DARK); }
    private static Font tableHeaderFont(){ return new Font(Font.HELVETICA, 9,  Font.BOLD, Color.WHITE); }
    private static Font footerFont()     { return new Font(Font.HELVETICA, 7,  Font.ITALIC, COLOR_GRAY); }
    private static Font bulletFont()     { return new Font(Font.HELVETICA, 9,  Font.NORMAL, COLOR_DARK); }

    // ─── Main Entry Point ─────────────────────────────────────────────────────

    /**
     * Generates a full PDF investment comparison report.
     *
     * @param request the investor's profile and transaction data
     * @return raw PDF bytes suitable for HTTP response
     */
    public byte[] generateReport(ReturnsRequest request) {
        log.info("Generating advanced PDF report for age={}, wage={}", request.getAge(), request.getWage());

        CompareResponse          compare      = compareService.compare(request);
        HybridResponse           hybrid       = hybridAllocationService.computeHybrid(request);
        RetirementIncomeResponse retirement   = retirementIncomeService.computeRetirementIncome(request);
        AlternativeInvestmentResponse alts    = alternativeInvestmentService.compute(request);

        ByteArrayOutputStream baos     = new ByteArrayOutputStream();
        Document              document = new Document(PageSize.A4, 50, 50, 55, 55);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // ── Page 1 : Header + KPI dashboard + Investor Profile ───────────
            addHeader(document, request);
            spacer(document);
            addKpiDashboard(document, compare, hybrid, retirement);
            spacer(document);
            addInvestorProfile(document, request, compare);

            // ── Page 2 : NPS and Index detailed tables + comparison chart ────
            document.newPage();
            addResultsTable(document, compare.getNps(),
                    "NPS — National Pension Scheme",
                    String.format("Annual Rate: %.2f%%  |  Tax Benefit under Section 80CCD",
                            ReturnCalculationService.NPS_RATE * 100),
                    true);
            spacer(document);
            addResultsTable(document, compare.getIndex(),
                    "NIFTY 50 Index Fund",
                    String.format("Annual Rate: %.2f%%  |  No tax benefit",
                            ReturnCalculationService.INDEX_RATE * 100),
                    false);
            spacer(document);
            addComparisonChart(document, writer, compare);

            // ── Page 3 : Hybrid analysis + Retirement Income Bridge ──────────
            document.newPage();
            addHybridSection(document, writer, hybrid);
            spacer(document);
            addRetirementIncomeSection(document, writer, retirement);

            // ── Page 4 : Breakeven + Risk Profile + Recommendation ───────────
            document.newPage();
            addBreakevenTable(document, compare, request);
            spacer(document);
            addRiskProfileTable(document);
            spacer(document);
            addRecommendation(document, compare);
            spacer(document);
            addSummaries(document, compare.getSummaries());

            // ── Page 5 : Alternative Investments comparison ──────────────────
            document.newPage();
            addAlternativesSection(document, writer, alts);

            addFooter(document);

        } catch (DocumentException | IOException e) {
            log.error("Failed to generate PDF: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        } finally {
            document.close();
        }

        log.info("Advanced PDF report generated: {} bytes", baos.size());
        return baos.toByteArray();
    }

    // ─── Sections ─────────────────────────────────────────────────────────────

    private void addHeader(Document doc, ReturnsRequest request) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_HEADER);
        cell.setPadding(18);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph title = new Paragraph("BlackRock Retirement Savings Report", titleFont());
        title.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(title);

        String generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        Paragraph sub = new Paragraph(
                "Auto-Savings Investment Analysis  ·  Generated: " + generated, subtitleFont());
        sub.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(sub);

        header.addCell(cell);
        doc.add(header);
    }

    private void addInvestorProfile(Document doc, ReturnsRequest request,
                                    CompareResponse compare) throws DocumentException {
        addSectionTitle(doc, "Investor Profile");

        int years = Math.max(
                ReturnCalculationService.RETIREMENT_AGE - request.getAge(),
                ReturnCalculationService.MIN_YEARS);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.3f, 1f, 1.5f, 1f, 1.4f, 1f});
        table.setSpacingBefore(4);

        addProfileCell(table, "Age");
        addProfileValue(table, request.getAge() + " yrs");
        addProfileCell(table, "Monthly Wage");
        addProfileValue(table, "₹" + format(request.getWage()));
        addProfileCell(table, "Annual Wage");
        addProfileValue(table, "₹" + format(request.getWage() * 12));

        addProfileCell(table, "Inflation");
        addProfileValue(table, request.getInflation() + "% p.a.");
        addProfileCell(table, "Yrs to Retire");
        addProfileValue(table, years + " years");
        addProfileCell(table, "Q / P / K");
        addProfileValue(table, sz(request.getQ()) + " / " + sz(request.getP()) + " / " + sz(request.getK()));

        addProfileCell(table, "Total Invested");
        addProfileValue(table, "₹" + format(compare.getTotalInvested()));
        addProfileCell(table, "Transactions");
        addProfileValue(table, String.valueOf(request.getTransactions().size()));
        addProfileCell(table, "");
        addProfileValue(table, "");

        doc.add(table);
    }

    private void addResultsTable(Document doc, ReturnsResponse response,
                                  String title, String subtitle,
                                  boolean showTax) throws DocumentException {
        addSectionTitle(doc, title);

        Font subFont = new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRAY);
        Paragraph sub = new Paragraph(subtitle, subFont);
        sub.setSpacingAfter(3);
        doc.add(sub);

        // Summary line
        Paragraph totals = new Paragraph();
        totals.add(new Chunk("Total Transactions: ", labelFont()));
        totals.add(new Chunk("₹" + format(response.getTotalTransactionAmount()) + "   ", valueFont()));
        totals.add(new Chunk("Total Ceiling: ", labelFont()));
        totals.add(new Chunk("₹" + format(response.getTotalCeiling()), valueFont()));
        totals.setSpacingAfter(5);
        doc.add(totals);

        // Table columns
        int cols = showTax ? 6 : 5;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        if (showTax) {
            table.setWidths(new float[]{2f, 2f, 1.2f, 1.2f, 1.4f, 1.4f});
        } else {
            table.setWidths(new float[]{2.5f, 2.5f, 1.5f, 1.5f, 1.5f});
        }

        // Header row
        String[] headers = showTax
                ? new String[]{"Period Start", "Period End", "Saved (₹)", "Profit (₹)", "Real Value (₹)", "Tax Benefit (₹)"}
                : new String[]{"Period Start", "Period End", "Saved (₹)", "Profit (₹)", "Real Value (₹)"};
        for (String h : headers) {
            addTh(table, h);
        }

        // Data rows
        if (response.getSavingsByDates() != null) {
            boolean alt = false;
            for (ReturnsResponse.SavingsByDate row : response.getSavingsByDates()) {
                Color bg = alt ? COLOR_ALT_ROW : Color.WHITE;
                addTd(table, fmtDate(row.getStart()), bg, Element.ALIGN_LEFT);
                addTd(table, fmtDate(row.getEnd()), bg, Element.ALIGN_LEFT);
                addTd(table, format(row.getAmount()), bg, Element.ALIGN_RIGHT);
                addTd(table, format(row.getProfit()), bg, Element.ALIGN_RIGHT);
                addTd(table, format(row.getAmount() + row.getProfit()), bg, Element.ALIGN_RIGHT);
                if (showTax) {
                    Font tf = new Font(Font.HELVETICA, 9, Font.NORMAL,
                            row.getTaxBenefit() > 0 ? COLOR_GREEN : COLOR_DARK);
                    PdfPCell tc = new PdfPCell(new Phrase(format(row.getTaxBenefit()), tf));
                    tc.setBackgroundColor(bg);
                    tc.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    tc.setPadding(5);
                    tc.setBorderColor(Color.LIGHT_GRAY);
                    table.addCell(tc);
                }
                alt = !alt;
            }
        }

        doc.add(table);
    }

    private void addRecommendation(Document doc, CompareResponse compare) throws DocumentException {
        addSectionTitle(doc, "Recommendation");

        boolean npsWins = "NPS".equals(compare.getRecommendation());
        Color badgeColor = npsWins ? COLOR_GREEN : COLOR_BLUE;

        PdfPTable badge = new PdfPTable(1);
        badge.setWidthPercentage(55);
        badge.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(badgeColor);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        Font bFont = new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE);
        cell.addElement(new Paragraph("✔  RECOMMENDED: " + compare.getRecommendation(), bFont));
        badge.addCell(cell);
        doc.add(badge);

        spacer(doc);

        Font rFont = new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_DARK);
        Paragraph reasoning = new Paragraph(compare.getReasoning(), rFont);
        reasoning.setLeading(16);
        doc.add(reasoning);
    }

    private void addSummaries(Document doc, List<String> summaries) throws DocumentException {
        if (summaries == null || summaries.isEmpty()) return;

        addSectionTitle(doc, "Period-by-Period Summary");

        for (String s : summaries) {
            Paragraph bullet = new Paragraph("•  " + s, bulletFont());
            bullet.setIndentationLeft(12);
            bullet.setLeading(15);
            doc.add(bullet);
        }
    }

    private void addFooter(Document doc) throws DocumentException {
        spacer(doc);
        LineSeparator line = new LineSeparator(0.5f, 100f, COLOR_GOLD, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(line));
        spacer(doc);

        Paragraph footer = new Paragraph(
                "Generated by BlackRock Retirement Auto-Savings API v1.0.0  |  " +
                "NPS Rate: 7.11% p.a.  |  NIFTY 50 Rate: 14.49% p.a.  |  " +
                "For illustrative purposes only — not financial advice.",
                footerFont());
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ─── Section Helpers ──────────────────────────────────────────────────────

    private void addSectionTitle(Document doc, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, sectionFont());
        title.setSpacingBefore(4);
        title.setSpacingAfter(2);
        doc.add(title);
        LineSeparator line = new LineSeparator(1f, 100f, COLOR_GOLD, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(line));
        spacer(doc);
    }

    private void spacer(Document doc) throws DocumentException {
        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));
    }

    // ─── Table Cell Helpers ───────────────────────────────────────────────────

    private void addTh(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, tableHeaderFont()));
        cell.setBackgroundColor(COLOR_HEADER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(Color.GRAY);
        table.addCell(cell);
    }

    private void addTd(PdfPTable table, String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, normalFont()));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addProfileCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, labelFont()));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        table.addCell(cell);
    }

    private void addProfileValue(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, valueFont()));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        table.addCell(cell);
    }

    // ─── Formatters ───────────────────────────────────────────────────────────

    private String format(Double value) {
        if (value == null) return "—";
        return String.format("%,.2f", value);
    }

    private String fmtDate(String dateStr) {
        if (dateStr == null) return "—";
        return dateStr.length() >= 10 ? dateStr.substring(0, 10) : dateStr;
    }

    private int sz(List<?> list) {
        return list == null ? 0 : list.size();
    }

    // ─── Comparison Bar Chart ─────────────────────────────────────────────────

    /**
     * Draws a horizontal bar chart comparing NPS effective return (profit + tax benefit)
     * against Index Fund effective return (profit only), using OpenPDF's low-level
     * PdfTemplate / PdfContentByte drawing API.
     */
    private void addComparisonChart(Document doc, PdfWriter writer, CompareResponse compare)
            throws DocumentException {
        addSectionTitle(doc, "NPS vs Index Fund — Visual Comparison");

        double npsEff = streamSum(compare.getNps(),   ReturnsResponse.SavingsByDate::getProfit)
                      + streamSum(compare.getNps(),   ReturnsResponse.SavingsByDate::getTaxBenefit);
        double idxEff = streamSum(compare.getIndex(), ReturnsResponse.SavingsByDate::getProfit);

        if (npsEff <= 0 && idxEff <= 0) {
            doc.add(new Paragraph("No k-period data available for chart.",
                    new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRAY)));
            return;
        }

        double  maxVal  = Math.max(npsEff, idxEff) * 1.18;   // 18% headroom
        boolean npsWins = npsEff >= idxEff;

        // ── Geometry ────────────────────────────────────────────────────────
        float W        = 490f;
        float H        = 135f;
        float lblW     = 108f;   // row-label column
        float valW     = 82f;    // value-label column (right)
        float barAreaW = W - lblW - valW;  // ≈ 300
        float barH     = 28f;
        float gapY     = 16f;
        float axisH    = 26f;    // space below bars for x-axis tick labels
        float npsY     = axisH + barH + gapY;  // upper bar bottom-Y
        float idxY     = axisH;                // lower bar bottom-Y
        float barX     = lblW;

        Color npsColor = new Color(0, 120, 55);   // green
        Color idxColor = new Color(0, 70, 150);   // blue

        // ── Create PdfTemplate canvas ─────────────────────────────────────────
        PdfContentByte cb  = writer.getDirectContent();
        PdfTemplate    tpl = cb.createTemplate(W, H);

        try {
            BaseFont bfReg  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            // ── Background ────────────────────────────────────────────────────
            tpl.setColorFill(new Color(250, 250, 250));
            tpl.rectangle(0, 0, W, H);
            tpl.fill();

            // ── Vertical grid lines + baseline ───────────────────────────────────
            int ticks = 5;
            tpl.setColorStroke(new Color(210, 210, 210));
            tpl.setLineWidth(0.4f);
            for (int i = 0; i <= ticks; i++) {
                float x = barX + barAreaW * i / ticks;
                tpl.moveTo(x, axisH - 3);
                tpl.lineTo(x, H - 6);
                tpl.stroke();
            }
            tpl.setColorStroke(new Color(170, 170, 170));
            tpl.setLineWidth(0.7f);
            tpl.moveTo(barX, axisH - 3);
            tpl.lineTo(barX + barAreaW, axisH - 3);
            tpl.stroke();

            // ── X-axis tick labels ────────────────────────────────────────────
            tpl.beginText();
            tpl.setFontAndSize(bfReg, 7f);
            tpl.setColorFill(COLOR_GRAY);
            for (int i = 0; i <= ticks; i++) {
                float  x   = barX + barAreaW * i / ticks;
                String lbl = formatShort(maxVal * i / ticks);
                float  lw  = lbl.length() * 3.8f;
                tpl.setTextMatrix(x - lw / 2f, axisH - 15);
                tpl.showText(lbl);
            }
            tpl.endText();

            // ── NPS bar (green) ───────────────────────────────────────────────
            float npsBarW = (float)(npsEff / maxVal * barAreaW);
            tpl.setColorFill(npsColor);
            tpl.rectangle(barX, npsY, npsBarW, barH);
            tpl.fill();

            // ── Index bar (blue) ───────────────────────────────────────────────
            float idxBarW = (float)(idxEff / maxVal * barAreaW);
            tpl.setColorFill(idxColor);
            tpl.rectangle(barX, idxY, idxBarW, barH);
            tpl.fill();

            // ── WINNER badge (white text inside winning bar) ──────────────────
            tpl.beginText();
            tpl.setFontAndSize(bfBold, 7.5f);
            tpl.setColorFill(Color.WHITE);
            float winY = (npsWins ? npsY : idxY) + barH / 2f - 4f;
            tpl.setTextMatrix(barX + 6, winY);
            tpl.showText("* WINNER");
            tpl.endText();

            // ── Row labels (left column) ────────────────────────────────────
            tpl.beginText();
            tpl.setFontAndSize(bfBold, 8.5f);
            tpl.setColorFill(npsColor);
            tpl.setTextMatrix(2, npsY + barH / 2f - 4f);
            tpl.showText("NPS (incl. tax)");
            tpl.setColorFill(idxColor);
            tpl.setTextMatrix(2, idxY + barH / 2f - 4f);
            tpl.showText("Index Fund");
            tpl.endText();

            // ── Value labels (right of each bar) ────────────────────────────
            tpl.beginText();
            tpl.setFontAndSize(bfBold, 8f);
            tpl.setColorFill(COLOR_DARK);
            tpl.setTextMatrix(barX + npsBarW + 5, npsY + barH / 2f - 4f);
            tpl.showText("Rs. " + formatShort(npsEff));
            tpl.setTextMatrix(barX + idxBarW + 5, idxY + barH / 2f - 4f);
            tpl.showText("Rs. " + formatShort(idxEff));
            tpl.endText();

        } catch (IOException e) {
            throw new DocumentException("Chart font error: " + e.getMessage());
        }

        // ── Embed template into document ──────────────────────────────────────
        Image chartImg = Image.getInstance(tpl);
        chartImg.setAlignment(Element.ALIGN_LEFT);
        doc.add(chartImg);

        // ── Legend ───────────────────────────────────────────────────────────
        PdfPTable legend = new PdfPTable(4);
        legend.setWidthPercentage(70);
        legend.setHorizontalAlignment(Element.ALIGN_LEFT);
        legend.setWidths(new float[]{0.4f, 2.8f, 0.4f, 2.8f});
        legend.setSpacingBefore(3);

        PdfPCell swNps = new PdfPCell(new Phrase(" "));
        swNps.setBackgroundColor(npsColor); swNps.setBorder(Rectangle.NO_BORDER); swNps.setFixedHeight(10);
        legend.addCell(swNps);
        PdfPCell lbNps = new PdfPCell(new Phrase("NPS — profit + tax benefit under Sec. 80CCD",
                new Font(Font.HELVETICA, 8, Font.NORMAL, COLOR_DARK)));
        lbNps.setBorder(Rectangle.NO_BORDER);
        legend.addCell(lbNps);

        PdfPCell swIdx = new PdfPCell(new Phrase(" "));
        swIdx.setBackgroundColor(idxColor); swIdx.setBorder(Rectangle.NO_BORDER); swIdx.setFixedHeight(10);
        legend.addCell(swIdx);
        PdfPCell lbIdx = new PdfPCell(new Phrase("Index Fund — profit only (no tax benefit)",
                new Font(Font.HELVETICA, 8, Font.NORMAL, COLOR_DARK)));
        lbIdx.setBorder(Rectangle.NO_BORDER);
        legend.addCell(lbIdx);

        doc.add(legend);
    }

    /** Sums a double field across all SavingsByDate rows of a ReturnsResponse. */
    private double streamSum(ReturnsResponse resp,
                             ToDoubleFunction<ReturnsResponse.SavingsByDate> fn) {
        if (resp == null || resp.getSavingsByDates() == null) return 0.0;
        return resp.getSavingsByDates().stream().mapToDouble(fn).sum();
    }

    /** Compact format for chart axis labels: 150000 → "1.5L", 12000 → "12.0K". */
    private String formatShort(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 100_000)   return String.format("%.1fL", v / 100_000);
        if (v >= 1_000)     return String.format("%.1fK", v / 1_000);
        return String.format("%.0f", v);
    }

    // ─── KPI Dashboard ────────────────────────────────────────────────────────

    /**
     * 6-box KPI dashboard row. Shows the most impactful numbers at a glance.
     * Layout: Total Invested | Hybrid Corpus | Monthly Income | Tax Saved | NPS Corpus | Index Corpus
     */
    private void addKpiDashboard(Document doc,
                                  CompareResponse compare,
                                  HybridResponse hybrid,
                                  RetirementIncomeResponse retirement) throws DocumentException {
        addSectionTitle(doc, "Executive KPI Dashboard");

        PdfPTable grid = new PdfPTable(3);
        grid.setWidthPercentage(100);
        grid.setSpacingBefore(4);

        Color[] bgColors = {
            new Color(31, 55, 100),   // Total Invested — navy
            new Color(0, 100, 45),    // Hybrid Corpus  — dark green
            new Color(0, 70, 140),    // Monthly Income — dark blue
            new Color(150, 100, 0),   // Tax Saved      — gold/amber
            new Color(60, 60, 60),    // NPS Corpus     — dark gray
            new Color(80, 40, 100),   // Index Corpus   — purple
        };
        String[] labels = {
            "TOTAL INVESTED",
            "HYBRID CORPUS",
            "MONTHLY INCOME",
            "ESTIMATED TAX SAVED",
            "NPS CORPUS",
            "INDEX CORPUS"
        };
        String[] values = {
            "\u20b9" + format(hybrid.getTotalInvested()),
            "\u20b9" + format(hybrid.getHybridCorpus()),
            "\u20b9" + format(retirement.getHybrid().getMonthlyIncome()),
            "\u20b9" + format(hybrid.getEstimatedTaxSaved()),
            "\u20b9" + format(hybrid.getNpsCorpus()),
            "\u20b9" + format(hybrid.getIndexCorpus()),
        };
        String[] subLabels = {
            "remanent across all transactions",
            "tax-optimal hybrid at retirement",
            "hybrid strategy per month",
            "via NPS Sec. 80CCD deduction",
            "NPS portion at retirement",
            "NIFTY 50 portion at retirement"
        };

        Font kpiVal = new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE);
        Font kpiLbl = new Font(Font.HELVETICA, 7,  Font.BOLD,
                new Color(200, 200, 200));
        Font kpiSub = new Font(Font.HELVETICA, 6, Font.ITALIC,
                new Color(170, 170, 170));

        for (int i = 0; i < 6; i++) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(bgColors[i]);
            cell.setPadding(10);
            cell.setBorderColor(new Color(60, 60, 60));
            cell.setBorderWidth(0.5f);

            Paragraph lbl = new Paragraph(labels[i], kpiLbl);
            lbl.setSpacingAfter(2);
            cell.addElement(lbl);

            Paragraph val = new Paragraph(values[i], kpiVal);
            val.setSpacingAfter(2);
            cell.addElement(val);

            Paragraph sub = new Paragraph(subLabels[i], kpiSub);
            cell.addElement(sub);

            grid.addCell(cell);
        }
        doc.add(grid);
    }

    // ─── Hybrid Analysis Section ──────────────────────────────────────────────

    private void addHybridSection(Document doc, PdfWriter writer,
                                   HybridResponse hybrid) throws DocumentException {
        addSectionTitle(doc, "Tax-First Hybrid Allocation Analysis");

        // Strategy description
        Font stratFont = new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_DARK);
        Paragraph strat = new Paragraph(hybrid.getAllocationStrategy(), stratFont);
        strat.setSpacingAfter(3);
        doc.add(strat);

        Font reasFont = new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRAY);
        Paragraph reas = new Paragraph(hybrid.getReasoning(), reasFont);
        reas.setSpacingAfter(6);
        reas.setLeading(14);
        doc.add(reas);

        // ── 3-column comparison table ─────────────────────────────────────────
        PdfPTable cmp = new PdfPTable(4);
        cmp.setWidthPercentage(100);
        cmp.setWidths(new float[]{2.2f, 1.6f, 1.6f, 1.6f});
        cmp.setSpacingBefore(4);

        // Header
        addTh(cmp, "Metric");
        addTh(cmp, "Pure NPS");
        addTh(cmp, "Pure Index");
        addThHighlight(cmp, "Tax-Hybrid");

        // Data rows
        Color alt = COLOR_ALT_ROW;
        addTd(cmp, "Corpus at Retirement",                   Color.WHITE, Element.ALIGN_LEFT);
        addTd(cmp, "\u20b9" + format(hybrid.getPureNpsCorpus()),   Color.WHITE, Element.ALIGN_RIGHT);
        addTd(cmp, "\u20b9" + format(hybrid.getPureIndexCorpus()), Color.WHITE, Element.ALIGN_RIGHT);
        addTdHighlight(cmp, "\u20b9" + format(hybrid.getHybridCorpus()));

        addTd(cmp, "Tax Saved",                              alt, Element.ALIGN_LEFT);
        addTd(cmp, "\u20b9" + format(hybrid.getEstimatedTaxSaved()), alt, Element.ALIGN_RIGHT);
        addTd(cmp, "\u20b90.00 (none)",                        alt, Element.ALIGN_RIGHT);
        addTdHighlight(cmp, "\u20b9" + format(hybrid.getEstimatedTaxSaved()));

        addTd(cmp, "Advantage vs this strategy",             Color.WHITE, Element.ALIGN_LEFT);
        addTd(cmp, "\u20b9" + format(hybrid.getHybridAdvantageOverNps()),   Color.WHITE, Element.ALIGN_RIGHT);
        addTd(cmp, "\u20b9" + format(hybrid.getHybridAdvantageOverIndex()), Color.WHITE, Element.ALIGN_RIGHT);
        addTdHighlight(cmp, "BEST STRATEGY ★");

        addTd(cmp, "NPS Allocation %",                       alt, Element.ALIGN_LEFT);
        addTd(cmp, String.format("%.1f%%", hybrid.getNpsAllocationPct()),   alt, Element.ALIGN_CENTER);
        addTd(cmp, "0.0%",                                   alt, Element.ALIGN_CENTER);
        addTdHighlight(cmp, String.format("%.1f%%", hybrid.getNpsAllocationPct()));

        addTd(cmp, "Index Fund Allocation %",                Color.WHITE, Element.ALIGN_LEFT);
        addTd(cmp, "0.0%",                                   Color.WHITE, Element.ALIGN_CENTER);
        addTd(cmp, "100.0%",                                 Color.WHITE, Element.ALIGN_CENTER);
        addTdHighlight(cmp, String.format("%.1f%%", hybrid.getIndexAllocationPct()));

        doc.add(cmp);
        spacer(doc);

        // ── Allocation split stacked bar ──────────────────────────────────────
        addAllocationBar(doc, writer, hybrid);
        spacer(doc);

        // ── Per-period breakdown table ─────────────────────────────────────────
        if (hybrid.getPeriodAllocations() != null && !hybrid.getPeriodAllocations().isEmpty()) {
            Font pTitle = new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_DARK);
            doc.add(new Paragraph("Per k-Period Allocation Breakdown", pTitle));

            PdfPTable pTable = new PdfPTable(7);
            pTable.setWidthPercentage(100);
            pTable.setWidths(new float[]{2f, 2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.4f});
            pTable.setSpacingBefore(4);
            for (String h : new String[]{"Period Start","Period End",
                    "Remanent","NPS Amt","Idx Amt","Tax Benefit","Routing"}) {
                addTh(pTable, h);
            }
            boolean a = false;
            for (HybridResponse.PeriodAllocation pa : hybrid.getPeriodAllocations()) {
                Color bg = a ? COLOR_ALT_ROW : Color.WHITE;
                addTd(pTable, fmtDate(pa.getStart()),          bg, Element.ALIGN_LEFT);
                addTd(pTable, fmtDate(pa.getEnd()),            bg, Element.ALIGN_LEFT);
                addTd(pTable, format(pa.getTotalRemanent()),   bg, Element.ALIGN_RIGHT);
                addTd(pTable, format(pa.getNpsAmount()),       bg, Element.ALIGN_RIGHT);
                addTd(pTable, format(pa.getIndexAmount()),     bg, Element.ALIGN_RIGHT);
                addTd(pTable, format(pa.getTaxBenefit()),      bg, Element.ALIGN_RIGHT);
                addTd(pTable, pa.getRouting(),                 bg, Element.ALIGN_CENTER);
                a = !a;
            }
            doc.add(pTable);
        }
    }

    /** Draws a horizontal stacked bar: NPS% (green) + Index% (blue). */
    private void addAllocationBar(Document doc, PdfWriter writer,
                                   HybridResponse hybrid) throws DocumentException {
        float W = 490f, H = 50f;
        float barY = 12f, barH2 = 22f;

        Color npsColor = new Color(0, 120, 55);
        Color idxColor = new Color(0, 70, 150);

        PdfContentByte cb  = writer.getDirectContent();
        PdfTemplate    tpl = cb.createTemplate(W, H);

        try {
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            float npsW = (float)(hybrid.getNpsAllocationPct() / 100.0 * W);
            float idxW = W - npsW;

            tpl.setColorFill(npsColor);
            tpl.rectangle(0, barY, npsW, barH2);
            tpl.fill();

            tpl.setColorFill(idxColor);
            tpl.rectangle(npsW, barY, idxW, barH2);
            tpl.fill();

            tpl.beginText();
            tpl.setFontAndSize(bfBold, 9f);
            tpl.setColorFill(Color.WHITE);
            tpl.setTextMatrix(6, barY + barH2 / 2f - 4f);
            tpl.showText(String.format("NPS  %.0f%%", hybrid.getNpsAllocationPct()));
            tpl.setTextMatrix(npsW + 6, barY + barH2 / 2f - 4f);
            tpl.showText(String.format("Index Fund  %.0f%%", hybrid.getIndexAllocationPct()));
            tpl.endText();

            tpl.beginText();
            tpl.setFontAndSize(bfBold, 7.5f);
            tpl.setColorFill(new Color(220, 220, 220));
            tpl.setTextMatrix(2, 2);
            tpl.showText("Allocation: ₹" + formatShort(hybrid.getNpsContribution())
                    + " NPS  |  ₹" + formatShort(hybrid.getIndexContribution()) + " Index Fund");
            tpl.endText();

        } catch (IOException e) {
            throw new DocumentException("Chart font error: " + e.getMessage());
        }

        Image img = Image.getInstance(tpl);
        img.setAlignment(Element.ALIGN_LEFT);
        doc.add(img);
    }

    // ─── Retirement Income Bridge ─────────────────────────────────────────────

    private void addRetirementIncomeSection(Document doc, PdfWriter writer,
                                             RetirementIncomeResponse retirement)
            throws DocumentException {
        addSectionTitle(doc, "Retirement Income Bridge — Corpus to Monthly Income");

        Font subFont = new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRAY);
        doc.add(new Paragraph(
                String.format("At retirement (age 60): projected monthly wage = ₹%.2f  |  " +
                        "Years to retirement: %d  |  Recommended: %s",
                        retirement.getProjectedMonthlyWageAtRetirement(),
                        retirement.getYearsToRetirement(),
                        retirement.getRecommendation()), subFont));
        spacer(doc);

        // ── Scenario comparison table ──────────────────────────────────────────
        PdfPTable tbl = new PdfPTable(5);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{2.2f, 1.4f, 1.2f, 1.6f, 2.8f});
        tbl.setSpacingBefore(4);

        addTh(tbl, "Strategy");
        addTh(tbl, "Monthly Income");
        addTh(tbl, "Annual Income");
        addTh(tbl, "Corpus");
        addTh(tbl, "Income Structure");

        RetirementIncomeResponse.IncomeScenario[] scenarios = {
                retirement.getNps(), retirement.getIndex(), retirement.getHybrid()
        };
        Color[] bgCols = { Color.WHITE, COLOR_ALT_ROW, new Color(230, 255, 230) };

        for (int i = 0; i < scenarios.length; i++) {
            RetirementIncomeResponse.IncomeScenario s = scenarios[i];
            Color bg = bgCols[i];
            boolean isRec = s.getName().contains("Hybrid") &&
                    "TAX_HYBRID".equals(retirement.getRecommendation());
            Font nameFont = new Font(Font.HELVETICA, 9, isRec ? Font.BOLD : Font.NORMAL, COLOR_DARK);

            PdfPCell nameCell = new PdfPCell(new Phrase(
                    (isRec ? "\u2605 " : "") + s.getName(), nameFont));
            nameCell.setBackgroundColor(bg); nameCell.setPadding(5);
            nameCell.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(nameCell);

            Font incomeFont = new Font(Font.HELVETICA, 10, Font.BOLD,
                    isRec ? COLOR_GREEN : COLOR_DARK);
            PdfPCell incCell = new PdfPCell(
                    new Phrase("₹" + format(s.getMonthlyIncome()), incomeFont));
            incCell.setBackgroundColor(bg); incCell.setPadding(5);
            incCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            incCell.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(incCell);

            addTd(tbl, "₹" + format(s.getAnnualIncome()),  bg, Element.ALIGN_RIGHT);
            addTd(tbl, "₹" + format(s.getTotalCorpus()),   bg, Element.ALIGN_RIGHT);
            addTd(tbl, s.getStructure(),                   bg, Element.ALIGN_LEFT);
        }
        doc.add(tbl);
        spacer(doc);

        // ── Reasoning paragraph ────────────────────────────────────────────────
        Paragraph reasoning = new Paragraph(retirement.getReasoning(),
                new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRAY));
        reasoning.setLeading(14);
        doc.add(reasoning);
        spacer(doc);

        // ── Monthly income bar chart ──────────────────────────────────────────
        addIncomeChart(doc, writer, retirement);
    }

    /** 3-bar horizontal chart comparing monthly income across scenarios. */
    private void addIncomeChart(Document doc, PdfWriter writer,
                                 RetirementIncomeResponse retirement) throws DocumentException {
        double[] values = {
                retirement.getNps().getMonthlyIncome(),
                retirement.getIndex().getMonthlyIncome(),
                retirement.getHybrid().getMonthlyIncome()
        };
        String[] names = { "NPS", "Index Fund", "Tax-Hybrid" };
        Color[]  cols  = {
                new Color(60, 100, 170),
                new Color(0, 130, 60),
                new Color(180, 100, 0)
        };

        double maxVal = Math.max(Math.max(values[0], values[1]), values[2]) * 1.2;
        if (maxVal <= 0) return;

        float W = 490f, H = 110f;
        float lblW = 90f, barAreaW = W - lblW - 70f, barH = 20f, gapY = 12f, axisH = 22f;
        float bar0Y = axisH + 2 * (barH + gapY);
        float bar1Y = axisH + (barH + gapY);
        float bar2Y = axisH;
        float barX  = lblW;

        PdfContentByte cb  = writer.getDirectContent();
        PdfTemplate    tpl = cb.createTemplate(W, H);

        try {
            BaseFont bfReg  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            tpl.setColorFill(new Color(250, 250, 250));
            tpl.rectangle(0, 0, W, H); tpl.fill();

            // Grid lines + baseline
            tpl.setColorStroke(new Color(210, 210, 210)); tpl.setLineWidth(0.4f);
            int ticks = 4;
            for (int i = 0; i <= ticks; i++) {
                float x = barX + barAreaW * i / ticks;
                tpl.moveTo(x, axisH - 2); tpl.lineTo(x, H - 4); tpl.stroke();
            }
            tpl.setColorStroke(new Color(170, 170, 170)); tpl.setLineWidth(0.7f);
            tpl.moveTo(barX, axisH - 2); tpl.lineTo(barX + barAreaW, axisH - 2); tpl.stroke();

            // Tick labels
            tpl.beginText(); tpl.setFontAndSize(bfReg, 7f); tpl.setColorFill(COLOR_GRAY);
            for (int i = 0; i <= ticks; i++) {
                float  x   = barX + barAreaW * i / ticks;
                String lbl = formatShort(maxVal * i / ticks);
                tpl.setTextMatrix(x - lbl.length() * 3.5f, axisH - 14);
                tpl.showText(lbl);
            }
            tpl.endText();

            float[] barYs = { bar0Y, bar1Y, bar2Y };
            for (int i = 0; i < 3; i++) {
                float bw = (float)(values[i] / maxVal * barAreaW);
                tpl.setColorFill(cols[i]);
                tpl.rectangle(barX, barYs[i], bw, barH); tpl.fill();

                // Row label
                tpl.beginText(); tpl.setFontAndSize(bfBold, 8f); tpl.setColorFill(COLOR_DARK);
                tpl.setTextMatrix(2, barYs[i] + barH / 2f - 4f);
                tpl.showText(names[i]); tpl.endText();

                // Value label (right of bar)
                tpl.beginText(); tpl.setFontAndSize(bfBold, 8f); tpl.setColorFill(COLOR_DARK);
                tpl.setTextMatrix(barX + bw + 5, barYs[i] + barH / 2f - 4f);
                tpl.showText("₹" + formatShort(values[i]) + "/mo"); tpl.endText();

                // RECOMMENDED badge
                boolean isRec = i == 2 && "TAX_HYBRID".equals(retirement.getRecommendation())
                             || i == 1 && "INDEX_FUND".equals(retirement.getRecommendation())
                             || i == 0 && "NPS".equals(retirement.getRecommendation());
                if (isRec) {
                    tpl.beginText(); tpl.setFontAndSize(bfBold, 7f); tpl.setColorFill(Color.WHITE);
                    tpl.setTextMatrix(barX + bw - 70, barYs[i] + barH / 2f - 4f);
                    tpl.showText("\u2605 RECOMMENDED"); tpl.endText();
                }
            }
        } catch (IOException e) {
            throw new DocumentException("Chart font error: " + e.getMessage());
        }

        Image img = Image.getInstance(tpl);
        img.setAlignment(Element.ALIGN_LEFT);
        doc.add(img);
    }

    // ─── Breakeven & Inflation Sensitivity Table ──────────────────────────────

    private void addBreakevenTable(Document doc, CompareResponse compare,
                                    ReturnsRequest request) throws DocumentException {
        addSectionTitle(doc, "Return vs Inflation Sensitivity Analysis");

        Font descFont = new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRAY);
        doc.add(new Paragraph(
                "How real (inflation-adjusted) returns change at different inflation rates. " +
                "NPS Tax Effective adds the estimated annual tax benefit as a % boost.", descFont));
        spacer(doc);

        double[] inflations = { 3, 4, 5, 6, 7, 8, 9, 10, 12 };
        double userInflation = request.getInflation();
        double annualWage    = request.getWage() * 12;
        double npsDeduction  = Math.min(ReturnCalculationService.MAX_NPS_DEDUCTION,
                ReturnCalculationService.NPS_DEDUCTION_PERCENT * annualWage);
        double taxBenefitPct = annualWage > 0
                ? compare.getNps().getSavingsByDates() != null &&
                  !compare.getNps().getSavingsByDates().isEmpty()
                  ? compare.getNps().getSavingsByDates().stream()
                        .mapToDouble(ReturnsResponse.SavingsByDate::getTaxBenefit).sum()
                    / Math.max(compare.getTotalInvested(), 1) * 100
                  : 0
                : 0;

        PdfPTable tbl = new PdfPTable(5);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{1.2f, 1.5f, 1.5f, 1.8f, 1.2f});
        tbl.setSpacingBefore(4);

        addTh(tbl, "Inflation");
        addTh(tbl, "NPS Real Return");
        addTh(tbl, "Index Real Return");
        addTh(tbl, "NPS+Tax Effective");
        addTh(tbl, "Winner");

        for (double infl : inflations) {
            double npsReal  = ReturnCalculationService.NPS_RATE   - infl / 100.0;
            double idxReal  = ReturnCalculationService.INDEX_RATE - infl / 100.0;
            double npsEff   = npsReal + taxBenefitPct / 100.0;

            boolean isUserRow = Math.abs(infl - userInflation) < 0.01;
            Color bg = isUserRow ? new Color(255, 255, 210) : (infl % 2 == 0 ? COLOR_ALT_ROW : Color.WHITE);

            String winner = npsEff > idxReal ? "NPS+Tax" : "Index";
            Color  winColor = winner.startsWith("NPS") ? COLOR_GREEN : COLOR_BLUE;

            addTd(tbl, String.format("%.0f%%%s", infl, isUserRow ? " ★" : ""), bg, Element.ALIGN_CENTER);
            addTd(tbl, String.format("%.2f%%",  npsReal  * 100), bg, Element.ALIGN_CENTER);
            addTd(tbl, String.format("%.2f%%",  idxReal  * 100), bg, Element.ALIGN_CENTER);
            addTd(tbl, String.format("%.2f%%",  npsEff   * 100), bg, Element.ALIGN_CENTER);

            PdfPCell wCell = new PdfPCell(new Phrase(winner,
                    new Font(Font.HELVETICA, 9, Font.BOLD, winColor)));
            wCell.setBackgroundColor(bg); wCell.setPadding(5);
            wCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            wCell.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(wCell);
        }
        doc.add(tbl);

        Paragraph note = new Paragraph(
                "★ = your current inflation rate  |  Real Return = Nominal Rate − Inflation  " +
                "(simplified linear approximation for quick comparison)",
                new Font(Font.HELVETICA, 7, Font.ITALIC, COLOR_GRAY));
        note.setSpacingBefore(3);
        doc.add(note);
    }

    // ─── Risk Profile Table ───────────────────────────────────────────────────

    private void addRiskProfileTable(Document doc) throws DocumentException {
        addSectionTitle(doc, "Risk Profile & Risk-Adjusted Return");

        Font descFont = new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRAY);
        doc.add(new Paragraph(
                "Sharpe-style score = (Expected Return − Risk-Free Rate 6.5%) / Annual Volatility. " +
                "Higher score = better risk-adjusted return.", descFont));
        spacer(doc);

        // Historical / estimated volatility values
        double rfRate  = 0.065;   // Indian 10Y G-Sec
        double npsVol  = 0.015;   // Government bond-heavy NPS
        double idxVol  = 0.150;   // NIFTY 50 historical std dev
        double npsR    = ReturnCalculationService.NPS_RATE;
        double idxR    = ReturnCalculationService.INDEX_RATE;
        double hybNpsP = 0.40;    // approximate NPS weight in hybrid (varies)
        double hybIdxP = 0.60;
        double hybR    = hybNpsP * npsR + hybIdxP * idxR;
        double hybVol  = hybNpsP * npsVol + hybIdxP * idxVol; // simplified (no correlation term)

        PdfPTable tbl = new PdfPTable(6);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{1.8f, 1.4f, 1.3f, 1.5f, 1.4f, 1.2f});
        tbl.setSpacingBefore(4);

        String[] headers = { "Strategy", "Exp. Return", "Volatility",
                "Sharpe Score", "Return Type", "Risk Level" };
        for (String h : headers) addTh(tbl, h);

        Object[][] rows = {
            { "NPS",           npsR, npsVol, (npsR - rfRate) / npsVol, "Fixed-income",  "LOW",     Color.WHITE    },
            { "NIFTY 50 Index",idxR, idxVol, (idxR - rfRate) / idxVol, "Equity",       "HIGH",    COLOR_ALT_ROW  },
            { "Tax-Hybrid",    hybR, hybVol, (hybR - rfRate) / hybVol, "Blended",      "MEDIUM",  new Color(230,255,230) },
        };

        for (Object[] row : rows) {
            Color bg = (Color) row[6];
            addTd(tbl, (String) row[0], bg, Element.ALIGN_LEFT);
            addTd(tbl, String.format("%.2f%%", ((double) row[1]) * 100), bg, Element.ALIGN_CENTER);
            addTd(tbl, String.format("%.1f%%",  ((double) row[2]) * 100), bg, Element.ALIGN_CENTER);

            double sharpe = (double) row[3];
            Color sharpeColor = sharpe > 0.5 ? COLOR_GREEN : sharpe > 0.3 ? COLOR_BLUE : COLOR_GRAY;
            PdfPCell sc = new PdfPCell(new Phrase(
                    String.format("%.2f", sharpe),
                    new Font(Font.HELVETICA, 9, Font.BOLD, sharpeColor)));
            sc.setBackgroundColor(bg); sc.setPadding(5);
            sc.setHorizontalAlignment(Element.ALIGN_CENTER);
            sc.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(sc);

            addTd(tbl, (String) row[4], bg, Element.ALIGN_CENTER);
            String risk = (String) row[5];
            Color riskColor = "LOW".equals(risk) ? COLOR_GREEN :
                              "HIGH".equals(risk) ? new Color(180, 0, 0) : COLOR_BLUE;
            PdfPCell rc = new PdfPCell(new Phrase(risk,
                    new Font(Font.HELVETICA, 9, Font.BOLD, riskColor)));
            rc.setBackgroundColor(bg); rc.setPadding(5);
            rc.setHorizontalAlignment(Element.ALIGN_CENTER);
            rc.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(rc);
        }
        doc.add(tbl);

        doc.add(new Paragraph(
                "Volatility: NPS ~1.5% (govt. bond benchmark), NIFTY 50 ~15% (historical). "
                + "Hybrid = weighted average (simplified, excludes correlation benefit).",
                new Font(Font.HELVETICA, 7, Font.ITALIC, COLOR_GRAY)));
    }

    // ─── Highlight cell helpers ───────────────────────────────────────────────

    /** Table header cell with gold/highlight background — for the recommended column. */
    private void addThHighlight(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, tableHeaderFont()));
        cell.setBackgroundColor(new Color(0, 100, 45));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(Color.GRAY);
        table.addCell(cell);
    }

    /** Data cell with light-green highlight — for the recommended column. */
    private void addTdHighlight(PdfPTable table, String text) {
        Font f = new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_GREEN);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(new Color(230, 255, 230));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    // ─── Page 5: Alternative Investments ─────────────────────────────────────

    /**
     * Adds a full page comparing corpus across 6 asset classes with a
     * horizontal bar chart and diversified portfolio recommendation.
     */
    private void addAlternativesSection(Document doc, PdfWriter writer,
                                        AlternativeInvestmentResponse alts)
            throws DocumentException, IOException {

        addSectionTitle(doc, "Alternative Investments — 6 Asset Class Comparison");

        doc.add(new Paragraph(String.format(
                "Inflation-adjusted (real) corpus at retirement for ₹%.0f invested over %d years at %.1f%% inflation.",
                alts.getTotalInvested(), alts.getYearsToRetirement(), alts.getInflationRatePct()),
                normalFont()));
        spacer(doc);

        // ── Comparison Table ──────────────────────────────────────────────────
        PdfPTable tbl = new PdfPTable(new float[]{22f, 10f, 16f, 16f, 12f, 12f, 12f});
        tbl.setWidthPercentage(100);

        String[] headers = {"Asset Class", "Rate %", "Real Corpus", "Real Profit", "ROI×", "Risk", "Liquidity"};
        for (String h : headers) addTh(tbl, h);

        List<AlternativeInvestmentResponse.AlternativeScenario> scenarios = List.of(
                alts.getNps(), alts.getIndexFund(), alts.getGold(),
                alts.getSilver(), alts.getBonds(), alts.getReits());

        double maxCorpus = scenarios.stream()
                .mapToDouble(AlternativeInvestmentResponse.AlternativeScenario::getRealCorpus)
                .max().orElse(1.0);

        int row = 0;
        for (AlternativeInvestmentResponse.AlternativeScenario s : scenarios) {
            Color bg = (row++ % 2 == 0) ? Color.WHITE : COLOR_ALT_ROW;
            boolean isTop = s.getRealCorpus() == maxCorpus;

            // Name with rank badge
            String nameText = "#" + s.getRank() + " " + s.getName();
            Font nameFont = isTop
                    ? new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_GREEN)
                    : new Font(Font.HELVETICA, 9, Font.NORMAL, COLOR_DARK);
            PdfPCell nc = new PdfPCell(new Phrase(nameText, nameFont));
            nc.setBackgroundColor(isTop ? new Color(230, 255, 230) : bg);
            nc.setPadding(5); nc.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(nc);

            addTd(tbl, String.format("%.2f%%", s.getAnnualRatePct()), bg, Element.ALIGN_CENTER);

            // Real Corpus — highlight top
            Font corpFont = isTop
                    ? new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_GREEN)
                    : valueFont();
            PdfPCell cc = new PdfPCell(new Phrase(format(s.getRealCorpus()), corpFont));
            cc.setBackgroundColor(isTop ? new Color(230, 255, 230) : bg);
            cc.setPadding(5); cc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cc.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(cc);

            addTd(tbl, format(s.getRealProfit()), bg, Element.ALIGN_RIGHT);
            addTd(tbl, String.format("%.2f×", s.getRoiMultiple()), bg, Element.ALIGN_CENTER);

            // Risk
            Color riskColor = "LOW".equals(s.getRiskLevel())  ? COLOR_GREEN
                            : "HIGH".equals(s.getRiskLevel()) ? new Color(180, 0, 0)
                            : COLOR_BLUE;
            PdfPCell rk = new PdfPCell(new Phrase(s.getRiskLevel(),
                    new Font(Font.HELVETICA, 9, Font.BOLD, riskColor)));
            rk.setBackgroundColor(bg); rk.setPadding(5);
            rk.setHorizontalAlignment(Element.ALIGN_CENTER);
            rk.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(rk);

            // Liquidity
            Color liqColor = "HIGH".equals(s.getLiquidity())  ? COLOR_GREEN
                           : "LOW".equals(s.getLiquidity())   ? new Color(180, 0, 0)
                           : COLOR_BLUE;
            PdfPCell lq = new PdfPCell(new Phrase(s.getLiquidity(),
                    new Font(Font.HELVETICA, 9, Font.BOLD, liqColor)));
            lq.setBackgroundColor(bg); lq.setPadding(5);
            lq.setHorizontalAlignment(Element.ALIGN_CENTER);
            lq.setBorderColor(Color.LIGHT_GRAY);
            tbl.addCell(lq);
        }
        doc.add(tbl);
        spacer(doc);

        // ── Horizontal Bar Chart ──────────────────────────────────────────────
        addSectionTitle(doc, "Corpus Comparison — Bar Chart");
        spacer(doc);   // gap between heading and first bar (fixes heading/NPS overlap)

        PdfContentByte cb     = writer.getDirectContent();
        float          pageW  = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
        float          barH   = 18f;
        float          gap    = 6f;
        float          labelW = 120f;
        float          chartW = pageW - labelW - 60f;

        String[] labels     = {"NPS", "NIFTY 50", "Gold (SGB)", "Silver", "GOI Bonds", "REITs"};
        double[] corpuses   = {alts.getNps().getRealCorpus(), alts.getIndexFund().getRealCorpus(),
                                alts.getGold().getRealCorpus(), alts.getSilver().getRealCorpus(),
                                alts.getBonds().getRealCorpus(), alts.getReits().getRealCorpus()};
        Color[] barColors   = {COLOR_BLUE, COLOR_GREEN, new Color(194, 148, 57),
                                new Color(160, 160, 160), new Color(80, 80, 200),
                                new Color(0, 140, 100)};

        // Use a PdfTemplate so document flow reserves the exact chart height
        // (fixes REITs / Diversified Portfolio overlap — no "\n" hacks needed)
        float       chartH = labels.length * (barH + gap) + 20f;   // 164 pt for 6 bars
        PdfTemplate tpl    = cb.createTemplate(pageW, chartH);

        for (int i = 0; i < labels.length; i++) {
            float y = chartH - 15f - i * (barH + gap);  // top-to-bottom within template

            // Label
            tpl.beginText();
            tpl.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false), 8);
            tpl.setColorFill(COLOR_DARK);
            tpl.setTextMatrix(0, y + 4);
            tpl.showText(labels[i]);
            tpl.endText();

            // Bar
            float barLen = maxCorpus > 0 ? (float) (corpuses[i] / maxCorpus * chartW) : 0;
            tpl.setColorFill(barColors[i]);
            tpl.rectangle(labelW, y, barLen, barH);
            tpl.fill();

            // Value label
            tpl.beginText();
            tpl.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false), 7);
            tpl.setColorFill(COLOR_DARK);
            tpl.setTextMatrix(labelW + barLen + 3, y + 5);
            tpl.showText(String.format("₹%.0f", corpuses[i]));
            tpl.endText();
        }

        Image img = Image.getInstance(tpl);
        img.setAlignment(Element.ALIGN_LEFT);
        doc.add(img);   // document flow now knows the exact chart height
        spacer(doc);

        // ── Diversified Portfolio Suggestion ─────────────────────────────────
        addSectionTitle(doc, "Suggested Diversified Portfolio");

        PdfPTable divTbl = new PdfPTable(new float[]{35f, 12f, 20f, 33f});
        divTbl.setWidthPercentage(100);
        addTh(divTbl, "Asset Class");
        addTh(divTbl, "Weight");
        addTh(divTbl, "Allocated (INR)");
        addTh(divTbl, "Rationale");

        Object[][] divRows = {
            {"NIFTY 50 Index Fund", "40%", alts.getTotalInvested() * 0.40, "Maximum long-term equity growth"},
            {"NPS (up to ₹2L cap)", "25%", alts.getTotalInvested() * 0.25, "Tax deduction + assured pension floor"},
            {"Gold — Sovereign Gold Bond", "15%", alts.getTotalInvested() * 0.15, "Inflation hedge + crisis protection"},
            {"REITs (Embassy/Brookfield)", "10%", alts.getTotalInvested() * 0.10, "Passive rental income + real estate"},
            {"Silver — MCX Digital", "7%",  alts.getTotalInvested() * 0.07, "Industrial demand + monetary metal"},
            {"GOI 10-yr Bonds",     "3%",  alts.getTotalInvested() * 0.03, "Capital safety + liquidity floor"},
        };

        int divRow = 0;
        for (Object[] dr : divRows) {
            Color bg = (divRow++ % 2 == 0) ? Color.WHITE : COLOR_ALT_ROW;
            addTd(divTbl, (String) dr[0], bg, Element.ALIGN_LEFT);
            addTd(divTbl, (String) dr[1], bg, Element.ALIGN_CENTER);
            addTd(divTbl, format((double) dr[2]), bg, Element.ALIGN_RIGHT);
            addTd(divTbl, (String) dr[3], bg, Element.ALIGN_LEFT);
        }
        doc.add(divTbl);
        spacer(doc);

        // Summary box
        PdfPTable sumBox = new PdfPTable(1);
        sumBox.setWidthPercentage(100);
        PdfPCell sc = new PdfPCell();
        sc.setBackgroundColor(new Color(240, 248, 255));
        sc.setPadding(10);
        sc.setBorderColor(COLOR_BLUE);

        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Diversified Portfolio Corpus: ",
                new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_BLUE)));
        sp.add(new Chunk(format(alts.getDiversifiedCorpus()) + "  (inflation-adjusted)",
                new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_DARK)));
        sc.addElement(sp);

        String advSign = alts.getDiversifiedAdvantageOverNps() >= 0 ? "+" : "";
        Paragraph ap = new Paragraph(
                String.format("Advantage over pure NPS: %s%s",
                        advSign, format(alts.getDiversifiedAdvantageOverNps())),
                new Font(Font.HELVETICA, 9, Font.NORMAL, COLOR_GREEN));
        sc.addElement(ap);

        Paragraph tp = new Paragraph(
                "Top Pick: " + alts.getTopPick() +
                "  |  Ranking: " + String.join(" > ",
                        alts.getRanking().stream()
                                .map(r -> r.replaceAll("^#\\d+ ", "").replaceAll(" —.*", ""))
                                .toList()),
                new Font(Font.HELVETICA, 8, Font.ITALIC, COLOR_GRAY));
        sc.addElement(tp);

        sumBox.addCell(sc);
        doc.add(sumBox);
    }
}

