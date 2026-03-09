package com.example.demo.service;

import com.example.demo.mapper.ExamResultMapper;
import com.example.demo.mapper.ExamSettingMapper;
import com.example.demo.mapper.LearningLogMapper;
import com.example.demo.mapper.QuestionAttemptMapper;
import com.example.demo.mapper.SubjectMapper;
import com.example.demo.mapper.WordMapper;
import com.example.demo.model.CorrectRateSummary;
import com.example.demo.model.ExamResult;
import com.example.demo.model.ExamSetting;
import com.example.demo.model.ForgettingReminderItem;
import com.example.demo.model.ImportBatchSummary;
import com.example.demo.model.LearningHeatmapCell;
import com.example.demo.model.MinorStudySignal;
import com.example.demo.model.MinorWrongCount;
import com.example.demo.model.PriorityLearningItem;
import com.example.demo.model.QuestionAttempt;
import com.example.demo.model.Word;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudyService {

    public static final int PASS_LINE = 70;
    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("yyyy/M/d");
    private static final DateTimeFormatter DATE_SLASH_WITH_TIME = DateTimeFormatter.ofPattern("yyyy/M/d H:mm");
    private static final Pattern HYPERLINK_PATTERN =
            Pattern.compile("^=HYPERLINK\\(\"([^\"]+)\",\"([^\"]+)\"\\)$", Pattern.CASE_INSENSITIVE);

    private final ExamSettingMapper examSettingMapper;
    private final ExamResultMapper examResultMapper;
    private final QuestionAttemptMapper questionAttemptMapper;
    private final SubjectMapper subjectMapper;
    private final WordMapper wordMapper;
    private final LearningLogMapper learningLogMapper;

    public StudyService(ExamSettingMapper examSettingMapper,
                        ExamResultMapper examResultMapper,
                        QuestionAttemptMapper questionAttemptMapper,
                        SubjectMapper subjectMapper,
                        WordMapper wordMapper,
                        LearningLogMapper learningLogMapper) {
        this.examSettingMapper = examSettingMapper;
        this.examResultMapper = examResultMapper;
        this.questionAttemptMapper = questionAttemptMapper;
        this.subjectMapper = subjectMapper;
        this.wordMapper = wordMapper;
        this.learningLogMapper = learningLogMapper;
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
            return "試験日まであと " + days + " 日";
        }
        if (days == 0) {
            return "今日は試験日です";
        }
        return "試験日から " + Math.abs(days) + " 日経過";
    }

    public List<ImportBatchSummary> findImportBatches() {
        return questionAttemptMapper.findImportBatches();
    }

    public List<ExamResult> findRecentResults(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return examResultMapper.findRecent(limit);
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
        persistDailyScoresForBatch(batchId, items.size());
        syncSubjectsFromAttempts(items);
        return items.size();
    }

    private void persistDailyScoresForBatch(String batchId, int limit) {
        List<ExamResult> dailyScores = questionAttemptMapper.findDailyScores(batchId, Math.max(1, limit));
        for (ExamResult score : dailyScores) {
            score.setSource("CSV Import");
            score.setMemo("importBatchId=" + batchId);
            examResultMapper.insert(score);
        }
    }

    public List<CorrectRateSummary> findFieldRates(String importBatchId) {
        return questionAttemptMapper.findFieldRates(null, normalizeBatch(importBatchId));
    }

    public List<CorrectRateSummary> findMinorRates(String importBatchId) {
        return questionAttemptMapper.findMinorRates(null, normalizeBatch(importBatchId));
    }

    public List<CorrectRateSummary> findBottleneckFieldsTop3(String importBatchId) {
        List<CorrectRateSummary> list = new ArrayList<>(findFieldRates(importBatchId));
        list.sort(Comparator.comparing(row -> row.getCorrectRate() != null ? row.getCorrectRate() : 0.0));
        return list.size() > 3 ? list.subList(0, 3) : list;
    }

    public List<String> findWeakMinorNames(int limit, String importBatchId) {
        List<CorrectRateSummary> list = questionAttemptMapper.findWorstMinorRates(null, normalizeBatch(importBatchId), limit);
        List<String> result = new ArrayList<>();
        for (CorrectRateSummary row : list) {
            if (row.getName() != null && !row.getName().isBlank()) {
                result.add(row.getName());
            }
        }
        return result;
    }

    public List<Word> findWeakWords(int limit) {
        return wordMapper.findWeakWords(limit);
    }

    public List<FieldImpact> calculateFieldImpacts(String importBatchId) {
        List<CorrectRateSummary> fieldRates = findFieldRates(importBatchId);
        long totalQuestions = fieldRates.stream()
                .map(row -> row.getTotalCount() != null ? row.getTotalCount() : 0L)
                .reduce(0L, Long::sum);
        if (totalQuestions <= 0) {
            return Collections.emptyList();
        }

        List<FieldImpact> impacts = new ArrayList<>();
        for (CorrectRateSummary row : fieldRates) {
            long count = row.getTotalCount() != null ? row.getTotalCount() : 0L;
            double weightRate = (double) count / (double) totalQuestions;
            double accuracy = row.getCorrectRate() != null ? row.getCorrectRate() : 0.0;
            double effectiveDelta = Math.min(10.0, Math.max(0.0, 100.0 - accuracy));
            double gainPoints = round1(weightRate * effectiveDelta);
            impacts.add(new FieldImpact(row.getName(), accuracy, count, round1(weightRate * 100.0), gainPoints));
        }
        impacts.sort(Comparator.comparing(FieldImpact::getGainPointsForPlus10Rate).reversed());
        return impacts;
    }

    public List<PriorityLearningItem> findTodayPriorityLearnings(String importBatchId, long daysUntilExam) {
        List<CorrectRateSummary> minorRates = findMinorRates(importBatchId);
        Map<String, CorrectRateSummary> rateMap = new HashMap<>();
        for (CorrectRateSummary row : minorRates) {
            if (row.getName() != null && !row.getName().isBlank()) {
                rateMap.put(row.getName(), row);
            }
        }

        Map<String, Long> recentWrongMap = new HashMap<>();
        for (MinorWrongCount row : questionAttemptMapper.findRecentWrongCountsByMinor(LocalDate.now().minusDays(14), normalizeBatch(importBatchId))) {
            recentWrongMap.put(row.getMinorName(), row.getWrongCount() != null ? row.getWrongCount() : 0L);
        }

        Map<String, MinorStudySignal> signalMap = new HashMap<>();
        for (MinorStudySignal signal : wordMapper.findMinorStudySignals()) {
            signalMap.put(signal.getMinorName(), signal);
        }

        Set<String> allMinors = new HashSet<>();
        allMinors.addAll(rateMap.keySet());
        allMinors.addAll(signalMap.keySet());

        double urgencyFactor = daysUntilExam <= 14 ? 1.25 : (daysUntilExam <= 30 ? 1.10 : 1.0);
        List<PriorityLearningItem> items = new ArrayList<>();
        for (String minor : allMinors) {
            CorrectRateSummary rate = rateMap.get(minor);
            MinorStudySignal signal = signalMap.get(minor);

            double correctRate = rate != null && rate.getCorrectRate() != null ? rate.getCorrectRate() : 50.0;
            long questionCount = rate != null && rate.getTotalCount() != null ? rate.getTotalCount() : 0L;
            long recentWrong = recentWrongMap.getOrDefault(minor, 0L);
            long wrongCount = signal != null && signal.getWrongCountTotal() != null ? signal.getWrongCountTotal() : 0L;
            long dueReviewCount = signal != null && signal.getDueReviewCount() != null ? signal.getDueReviewCount() : 0L;

            double score = ((100.0 - correctRate) * 0.45
                    + Math.log1p(questionCount) * 8.0
                    + recentWrong * 8.0
                    + wrongCount * 0.9
                    + dueReviewCount * 4.0) * urgencyFactor;

            items.add(new PriorityLearningItem(minor, round1(correctRate), questionCount, recentWrong, wrongCount, dueReviewCount, round1(score)));
        }

        items.sort(Comparator.comparing(PriorityLearningItem::getPriorityScore).reversed());
        return items.size() > 3 ? items.subList(0, 3) : items;
    }

    public List<ForgettingReminderItem> findForgettingReminders(int limit) {
        return wordMapper.findForgettingReminderItems(limit);
    }

    public List<LearningHeatmapCell> findLearningHeatmap(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(1, days) - 1L);
        return learningLogMapper.findDailyCounts(from, to);
    }

    public PassForecast calculatePassForecast(List<ExamResult> recentDesc) {
        if (recentDesc == null || recentDesc.isEmpty()) {
            return new PassForecast(0.0, 0.0);
        }

        double latest = recentDesc.get(0).getScoreTotal() != null ? recentDesc.get(0).getScoreTotal() : 0.0;
        List<Double> scoresAsc = new ArrayList<>();
        int take = Math.min(3, recentDesc.size());
        for (int i = take - 1; i >= 0; i--) {
            Integer s = recentDesc.get(i).getScoreTotal();
            scoresAsc.add(s != null ? s.doubleValue() : 0.0);
        }

        double slope = 0.0;
        if (scoresAsc.size() >= 2) {
            slope = (scoresAsc.get(scoresAsc.size() - 1) - scoresAsc.get(0)) / (scoresAsc.size() - 1);
        }
        double predicted = round1(latest + slope);

        double probability = 50.0 + ((predicted - PASS_LINE) * 3.0) + (slope * 2.0);
        probability = Math.max(1.0, Math.min(99.0, probability));
        return new PassForecast(round1(probability), predicted);
    }

    public int latestScore(List<ExamResult> recentDesc) {
        if (recentDesc == null || recentDesc.isEmpty() || recentDesc.get(0).getScoreTotal() == null) {
            return 0;
        }
        return recentDesc.get(0).getScoreTotal();
    }

    public int previousDiff(List<ExamResult> recentDesc) {
        if (recentDesc == null || recentDesc.size() < 2) {
            return 0;
        }
        int latest = recentDesc.get(0).getScoreTotal() != null ? recentDesc.get(0).getScoreTotal() : 0;
        int prev = recentDesc.get(1).getScoreTotal() != null ? recentDesc.get(1).getScoreTotal() : 0;
        return latest - prev;
    }

    private void syncSubjectsFromAttempts(List<QuestionAttempt> items) {
        Set<String> seen = new HashSet<>();
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("Strategy", "#fd7e14");
        colorMap.put("Management", "#0d6efd");
        colorMap.put("Technology", "#198754");

        for (QuestionAttempt item : items) {
            String key = item.getFieldName() + "|" + item.getMajorName() + "|" + item.getMinorName();
            if (!seen.add(key)) {
                continue;
            }
            String color = colorMap.getOrDefault(item.getFieldName(), "#6c757d");
            subjectMapper.mergeFromCategory(item.getFieldName(), item.getMajorName(), item.getMinorName(), color);
        }
    }

    private String normalizeBatch(String importBatchId) {
        if (importBatchId == null || importBatchId.isBlank() || "all".equals(importBatchId)) {
            return null;
        }
        return importBatchId;
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
        return head.contains("No.");
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

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
