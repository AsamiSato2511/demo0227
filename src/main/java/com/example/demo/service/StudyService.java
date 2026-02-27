package com.example.demo.service;

import com.example.demo.mapper.ExamResultMapper;
import com.example.demo.mapper.ExamSettingMapper;
import com.example.demo.mapper.QuestionAttemptMapper;
import com.example.demo.mapper.SubjectMapper;
import com.example.demo.model.CategoryRateRow;
import com.example.demo.model.CorrectRateSummary;
import com.example.demo.model.ExamResult;
import com.example.demo.model.ExamSetting;
import com.example.demo.model.QuestionAttempt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudyService {

    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("yyyy/M/d");
    private static final DateTimeFormatter DATE_SLASH_WITH_TIME = DateTimeFormatter.ofPattern("yyyy/M/d H:mm");
    private static final Pattern HYPERLINK_PATTERN =
            Pattern.compile("^=HYPERLINK\\(\"([^\"]+)\",\"([^\"]+)\"\\)$", Pattern.CASE_INSENSITIVE);

    private final ExamSettingMapper examSettingMapper;
    private final ExamResultMapper examResultMapper;
    private final QuestionAttemptMapper questionAttemptMapper;
    private final SubjectMapper subjectMapper;

    public StudyService(ExamSettingMapper examSettingMapper,
                        ExamResultMapper examResultMapper,
                        QuestionAttemptMapper questionAttemptMapper,
                        SubjectMapper subjectMapper) {
        this.examSettingMapper = examSettingMapper;
        this.examResultMapper = examResultMapper;
        this.questionAttemptMapper = questionAttemptMapper;
        this.subjectMapper = subjectMapper;
    }

    public ExamSetting getExamSetting() {
        return examSettingMapper.findOne();
    }

    public boolean updateExamDate(LocalDate examDate) {
        if (examDate == null) {
            return false;
        }
        return examSettingMapper.updateExamDate(examDate) > 0;
    }

    public long getDaysUntilExam(LocalDate examDate) {
        if (examDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), examDate);
    }

    public String buildCountdownMessage(long days) {
        if (days > 0) {
            return "試験まであと" + days + "日";
        }
        if (days == 0) {
            return "試験当日です";
        }
        return "試験日から" + Math.abs(days) + "日経過";
    }

    public List<ExamResult> findRecentResults(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return examResultMapper.findRecent(limit);
    }

    public Map<String, Object> findResultStats() {
        return examResultMapper.findStats();
    }

    public void createResult(ExamResult examResult) {
        examResultMapper.insert(examResult);
    }

    public int importQuestionAttempts(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return 0;
        }
        String csvText = decodeCsv(file.getBytes());
        List<QuestionAttempt> items = new ArrayList<>();
        String batchId = UUID.randomUUID().toString();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvText))) {
            String line;
            boolean firstRow = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (firstRow) {
                    firstRow = false;
                    line = removeBom(line);
                    if (line.startsWith("No.")) {
                        continue;
                    }
                }
                List<String> cols = splitCsvLine(line);
                if (cols.size() < 7) {
                    continue;
                }
                QuestionAttempt item = new QuestionAttempt();
                item.setAttemptedOn(parseDate(cols.get(6)));
                item.setCorrect(parseCorrect(cols.get(1)));
                item.setFieldName(cols.get(2).trim());
                item.setMajorName(cols.get(3).trim());
                item.setMinorName(cols.get(4).trim());
                applyHyperlink(cols.get(5), item);
                item.setImportBatchId(batchId);
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return 0;
        }
        questionAttemptMapper.insertBatch(items);
        syncSubjectsFromAttempts(items);
        reflectUnderstandingFromMinorRates();
        return items.size();
    }

    public List<CorrectRateSummary> findFieldRates(String period) {
        return questionAttemptMapper.findFieldRates(resolveFromDate(period));
    }

    public List<CorrectRateSummary> findMinorRates(String period) {
        return questionAttemptMapper.findMinorRates(resolveFromDate(period));
    }

    public List<CorrectRateSummary> findWorstMinorRates(String period, int limit) {
        return questionAttemptMapper.findWorstMinorRates(resolveFromDate(period), limit);
    }

    public List<QuestionAttempt> findRecentAttempts(String period, int limit) {
        return questionAttemptMapper.findRecent(resolveFromDate(period), limit);
    }

    private void reflectUnderstandingFromMinorRates() {
        List<CategoryRateRow> rates = questionAttemptMapper.findCategoryRates(null);
        for (CategoryRateRow row : rates) {
            if (row.getMinorName() == null || row.getCorrectRate() == null) {
                continue;
            }
            int understanding = (int) Math.round(row.getCorrectRate());
            subjectMapper.updateUnderstandingByCategory(
                    row.getFieldName(),
                    row.getMajorName(),
                    row.getMinorName(),
                    understanding
            );
        }
    }

    private void syncSubjectsFromAttempts(List<QuestionAttempt> items) {
        Set<String> seen = new HashSet<>();
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("ストラテジ系", "#fd7e14");
        colorMap.put("マネジメント系", "#0d6efd");
        colorMap.put("テクノロジ系", "#198754");

        for (QuestionAttempt item : items) {
            String key = item.getFieldName() + "|" + item.getMajorName() + "|" + item.getMinorName();
            if (!seen.add(key)) {
                continue;
            }
            String color = colorMap.getOrDefault(item.getFieldName(), "#6c757d");
            subjectMapper.mergeFromCategory(item.getFieldName(), item.getMajorName(), item.getMinorName(), color);
        }
    }

    private String decodeCsv(byte[] bytes) {
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (looksLikeExpectedCsv(utf8)) {
            return utf8;
        }
        Charset ms932 = Charset.forName("MS932");
        String sjis = new String(bytes, ms932);
        if (looksLikeExpectedCsv(sjis)) {
            return sjis;
        }
        return utf8;
    }

    private boolean looksLikeExpectedCsv(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.indexOf('\uFFFD') >= 0) {
            return false;
        }
        String head = text.length() > 200 ? text.substring(0, 200) : text;
        return head.contains("No.") && head.contains("正誤") && head.contains("分野名");
    }

    private LocalDate resolveFromDate(String period) {
        if ("30d".equals(period)) {
            return LocalDate.now().minusDays(30);
        }
        if ("7d".equals(period)) {
            return LocalDate.now().minusDays(7);
        }
        return null;
    }

    private void applyHyperlink(String raw, QuestionAttempt item) {
        String value = raw == null ? "" : raw.trim();
        Matcher matcher = HYPERLINK_PATTERN.matcher(value);
        if (matcher.matches()) {
            item.setSourceUrl(matcher.group(1));
            item.setSourceLabel(matcher.group(2));
        } else {
            item.setSourceLabel(value);
            item.setSourceUrl(null);
        }
    }

    private Boolean parseCorrect(String value) {
        String trimmed = value == null ? "" : value.trim();
        return "○".equals(trimmed)
                || "◯".equals(trimmed)
                || "o".equalsIgnoreCase(trimmed)
                || "1".equals(trimmed)
                || "true".equalsIgnoreCase(trimmed);
    }

    private LocalDate parseDate(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(trimmed, DATE_SLASH);
        } catch (DateTimeParseException ignore) {
        }
        try {
            return LocalDateTime.parse(trimmed, DATE_SLASH_WITH_TIME).toLocalDate();
        } catch (DateTimeParseException ignore) {
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignore) {
            return LocalDate.now();
        }
    }

    private String removeBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }
}
