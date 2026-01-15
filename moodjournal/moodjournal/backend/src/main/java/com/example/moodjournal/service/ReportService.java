package com.example.moodjournal.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.JournalEntryRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class ReportService {

    private final JournalEntryRepository journalEntryRepository;

    public ReportService(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    public byte[] generateMonthlyReport(User user) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts - Using generic names to avoid potential missing font issues
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font moodFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLUE);

            // Title
            Paragraph title = new Paragraph("Mood Journal Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // User Info (Safe Access)
            String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "User";
            String email = (user != null && user.getEmail() != null) ? user.getEmail() : "";
            Paragraph userInfo = new Paragraph("User: " + name + " (" + email + ")");
            userInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(userInfo);
            document.add(Chunk.NEWLINE);

            // Fetch entries safely using ID
            Long userId = (user != null) ? user.getId() : -1L;
            List<JournalEntry> entries = journalEntryRepository.findByUserId(userId);

            // Manual sort to fallback if DB sort fails or repository method is missing
            try {
                if (entries != null) {
                    entries.sort((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null)
                            return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                }
            } catch (Exception e) {
                // Ignore sort errors
            }

            if (entries == null || entries.isEmpty()) {
                document.add(new Paragraph("No accessible journal entries found."));
            } else {
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[] { 3, 4, 3, 2 });
                table.setSpacingBefore(10f);

                addTableHeader(table, "Date", headerFont);
                addTableHeader(table, "Title", headerFont);
                addTableHeader(table, "Mood", headerFont);
                addTableHeader(table, "Stress", headerFont);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                for (JournalEntry entry : entries) {
                    try {
                        String dateStr = "-";
                        if (entry.getCreatedAt() != null) {
                            LocalDateTime ldt = LocalDateTime.ofInstant(entry.getCreatedAt(), ZoneId.systemDefault());
                            dateStr = ldt.format(formatter);
                        }
                        String titleStr = entry.getTitle() != null ? entry.getTitle() : "Untitled";
                        String moodStr = entry.getMood() != null ? entry.getMood().toString() : "N/A";
                        String stressStr = entry.getStressLevel() != null ? String.valueOf(entry.getStressLevel())
                                : "-";

                        table.addCell(new Phrase(dateStr, bodyFont));
                        table.addCell(new Phrase(titleStr, bodyFont));
                        table.addCell(new Phrase(moodStr, moodFont));
                        table.addCell(new Phrase(stressStr, bodyFont));
                    } catch (Exception innerEx) {
                        continue;
                    }
                }
                document.add(table);
            }

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: Create a simple error PDF so the user sees *something*
            try {
                Document errorDoc = new Document();
                ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
                PdfWriter.getInstance(errorDoc, errorOut);
                errorDoc.open();
                errorDoc.add(new Paragraph("Error generating report: " + e.getMessage()));
                errorDoc.close();
                return errorOut.toByteArray();
            } catch (Exception ex) {
                return new byte[0];
            }
        }

        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String headerTitle, Font font) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(Color.DARK_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerTitle, font));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.setPadding(5);
        table.addCell(header);
    }
}
