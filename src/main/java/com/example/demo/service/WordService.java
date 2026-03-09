package com.example.demo.service;

import com.example.demo.mapper.LearningLogMapper;
import com.example.demo.mapper.WordMapper;
import com.example.demo.model.Word;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WordService {

    public static final String MODE_NORMAL = "NORMAL";
    public static final String MODE_REVIEW = "REVIEW";
    private static final String STATUS_REVIEW = "要復習";
    private static final String STATUS_LEARNING = "学習中";
    private static final int QUIZ_SIZE = 10;

    private final WordMapper wordMapper;
    private final LearningLogMapper learningLogMapper;
    private final Random random = new Random();

    public WordService(WordMapper wordMapper, LearningLogMapper learningLogMapper) {
        this.wordMapper = wordMapper;
        this.learningLogMapper = learningLogMapper;
    }

    public List<Word> findAll(String keyword, String minorName, String status, String importance) {
        return wordMapper.findAll(keyword, minorName, status, importance);
    }

    public List<String> findMinorNames() {
        return wordMapper.findMinorNames();
    }

    public Word findById(Long id) {
        if (id == null) {
            return null;
        }
        return wordMapper.findById(id);
    }

    public List<Word> findWeakWords(int limit) {
        return wordMapper.findWeakWords(Math.max(1, limit));
    }

    public boolean updateById(Word word) {
        if (word == null || word.getId() == null) {
            return false;
        }
        if (isBlank(word.getTerm()) || isBlank(word.getMeaning()) || isBlank(word.getMinorName())) {
            return false;
        }
        word.setStatus(normalizeStatus(word.getStatus()));
        word.setWrongCount(Math.max(0, Objects.requireNonNullElse(word.getWrongCount(), 0)));
        if (word.getQuizEnabled() == null) {
            word.setQuizEnabled(true);
        }
        return wordMapper.updateById(word) > 0;
    }

    public int excludeFromQuiz(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return wordMapper.updateQuizEnabledByIds(ids, false);
    }

    public boolean updateQuizEnabled(Long id, boolean enabled) {
        if (id == null) {
            return false;
        }
        return wordMapper.updateQuizEnabledById(id, enabled) > 0;
    }

    public CsvImportResult importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new CsvImportResult(0, 0);
        }

        int imported = 0;
        int skipped = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }

                List<String> cols = splitCsvLine(line);
                if (cols.size() < 10) {
                    skipped++;
                    continue;
                }

                Word w = new Word();
                w.setTerm(trimOrEmpty(cols, 0));
                w.setMeaning(trimOrEmpty(cols, 1));
                w.setChoice1(trimOrNull(cols, 2));
                w.setChoice2(trimOrNull(cols, 3));
                w.setChoice3(trimOrNull(cols, 4));
                w.setChoice4(trimOrNull(cols, 5));
                w.setAnswerIndex(parseAnswerIndex(trimOrNull(cols, 6)));
                w.setMinorName(trimOrEmpty(cols, 7));
                w.setStatus(normalizeStatus(trimOrEmpty(cols, 8)));
                w.setWrongCount(parseInt(trimOrEmpty(cols, 9), 0));
                w.setFieldName("未分類");
                w.setMajorName("未分類");
                w.setQuizEnabled(true);
                w.setPriority(0);

                if (!isImportable(w)) {
                    skipped++;
                    continue;
                }
                wordMapper.upsert(w);
                imported++;
            }
        }
        return new CsvImportResult(imported, skipped);
    }

    public String sampleCsv() {
        return "term,meaning,choice1,choice2,choice3,choice4,answer,minor_name,status,wrong_count\n"
                + "PKI,公開鍵基盤の仕組み,公開鍵証明書,排他制御,ER図,スループット,1,セキュリティ,要復習,3\n"
                + "CAPEX,設備投資の意味,固定資産への投資,運転資金,減価償却費,税額控除,1,企業活動,学習中,1\n";
    }

    public QuizState createQuiz(String minorName) {
        List<Word> pool = wordMapper.findAllPool(minorName);
        if (pool.isEmpty()) {
            return new QuizState(Collections.emptyList(), MODE_NORMAL);
        }
        int count = Math.min(QUIZ_SIZE, pool.size());
        List<Word> selected = pickPrioritizedWords(pool, count);
        return new QuizState(buildQuestions(selected, pool), MODE_NORMAL);
    }

    public QuizState createQuizFromWrongQuestions(List<QuizQuestion> wrongQuestions) {
        if (wrongQuestions == null || wrongQuestions.isEmpty()) {
            return new QuizState(Collections.emptyList(), MODE_REVIEW);
        }

        List<Word> pool = wordMapper.findAllPool(null);
        Map<Long, Word> byId = pool.stream().collect(Collectors.toMap(Word::getId, w -> w, (a, b) -> a));
        List<Word> seed = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        for (QuizQuestion q : wrongQuestions) {
            if (q.getWordId() == null || !used.add(q.getWordId())) {
                continue;
            }
            Word w = byId.get(q.getWordId());
            if (w != null) {
                seed.add(w);
            }
        }

        List<Word> sortedByWrong = new ArrayList<>(pool);
        sortedByWrong.sort(Comparator.comparing((Word w) -> Objects.requireNonNullElse(w.getWrongCount(), 0)).reversed());
        for (Word w : sortedByWrong) {
            if (seed.size() >= Math.min(QUIZ_SIZE, pool.size())) {
                break;
            }
            if (w.getId() != null && used.add(w.getId())) {
                seed.add(w);
            }
        }

        return new QuizState(buildQuestions(seed, pool), MODE_REVIEW);
    }

    public boolean answer(QuizState state, int selectedIndex) {
        if (state == null || state.isFinished()) {
            return false;
        }
        QuizQuestion q = state.current();
        if (q.isAnswered()) {
            return q.isCorrect();
        }
        q.setAnswered(true);
        q.setSelectedIndex(selectedIndex);
        boolean correct = selectedIndex >= 0
                && selectedIndex < q.getOptions().size()
                && q.getCorrectMeaning().equals(q.getOptions().get(selectedIndex));
        q.setCorrect(correct);

        if (correct) {
            state.setCorrectCount(state.getCorrectCount() + 1);
            wordMapper.markCorrect(q.getWordId());
        } else {
            state.getWrongQuestions().add(q);
            wordMapper.incrementWrongCount(q.getWordId());
        }

        String actionType = MODE_REVIEW.equals(state.getMode()) ? "REVIEW" : "QUIZ_ANSWER";
        learningLogMapper.insert(actionType, q.getWordId(), LocalDateTime.now(), correct);
        return correct;
    }

    public void next(QuizState state) {
        if (state == null) {
            return;
        }
        if (state.getIndex() < state.getQuestions().size() - 1) {
            state.setIndex(state.getIndex() + 1);
        } else {
            state.setFinished(true);
        }
    }

    private List<Word> pickPrioritizedWords(List<Word> pool, int count) {
        Map<Integer, List<Word>> grouped = pool.stream().collect(Collectors.groupingBy(w -> Objects.requireNonNullElse(w.getWrongCount(), 0)));
        List<Integer> wrongCounts = new ArrayList<>(grouped.keySet());
        wrongCounts.sort(Comparator.reverseOrder());

        List<Word> selected = new ArrayList<>();
        for (Integer wrongCount : wrongCounts) {
            List<Word> bucket = new ArrayList<>(grouped.getOrDefault(wrongCount, Collections.emptyList()));
            Collections.shuffle(bucket, random);
            for (Word word : bucket) {
                if (selected.size() >= count) {
                    return selected;
                }
                selected.add(word);
            }
        }
        return selected;
    }

    private List<QuizQuestion> buildQuestions(List<Word> selected, List<Word> allPool) {
        List<QuizQuestion> questions = new ArrayList<>();
        for (Word target : selected) {
            String correctMeaning = resolveCorrectMeaning(target);
            List<String> options = hasFixedQuizOptions(target)
                    ? fixedOptions(target)
                    : buildRandomOptions(target, allPool, correctMeaning);
            questions.add(new QuizQuestion(target.getId(), target.getTerm(), correctMeaning, options));
        }
        return questions;
    }

    private List<String> fixedOptions(Word target) {
        List<String> options = new ArrayList<>(4);
        options.add(target.getChoice1());
        options.add(target.getChoice2());
        options.add(target.getChoice3());
        options.add(target.getChoice4());
        return options;
    }

    private String resolveCorrectMeaning(Word target) {
        if (hasFixedQuizOptions(target)) {
            List<String> options = fixedOptions(target);
            return options.get(target.getAnswerIndex() - 1);
        }
        return target.getMeaning();
    }

    private boolean hasFixedQuizOptions(Word target) {
        return target.getAnswerIndex() != null
                && target.getAnswerIndex() >= 1
                && target.getAnswerIndex() <= 4
                && !isBlank(target.getChoice1())
                && !isBlank(target.getChoice2())
                && !isBlank(target.getChoice3())
                && !isBlank(target.getChoice4());
    }

    private List<String> buildRandomOptions(Word target, List<Word> allPool, String correctMeaning) {
        Set<String> used = new HashSet<>();
        List<String> options = new ArrayList<>();
        options.add(correctMeaning);
        used.add(correctMeaning);

        List<Word> sameMinor = allPool.stream()
                .filter(w -> Objects.equals(w.getMinorName(), target.getMinorName()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(sameMinor, random);
        for (Word cand : sameMinor) {
            if (options.size() >= 4) {
                break;
            }
            if (Objects.equals(cand.getId(), target.getId()) || isBlank(cand.getMeaning()) || used.contains(cand.getMeaning())) {
                continue;
            }
            options.add(cand.getMeaning());
            used.add(cand.getMeaning());
        }

        if (options.size() < 4) {
            List<Word> all = new ArrayList<>(allPool);
            Collections.shuffle(all, random);
            for (Word cand : all) {
                if (options.size() >= 4) {
                    break;
                }
                if (Objects.equals(cand.getId(), target.getId()) || isBlank(cand.getMeaning()) || used.contains(cand.getMeaning())) {
                    continue;
                }
                options.add(cand.getMeaning());
                used.add(cand.getMeaning());
            }
        }
        Collections.shuffle(options, random);
        return options;
    }

    private String normalizeStatus(String raw) {
        if (STATUS_REVIEW.equals(raw)) {
            return STATUS_REVIEW;
        }
        return STATUS_LEARNING;
    }

    private boolean isImportable(Word w) {
        if (isBlank(w.getTerm()) || isBlank(w.getMeaning()) || isBlank(w.getMinorName())) {
            return false;
        }
        if (w.getWrongCount() == null || w.getWrongCount() < 0) {
            w.setWrongCount(0);
        }
        return true;
    }

    private Integer parseAnswerIndex(String value) {
        Integer parsed = parseNullableInt(value);
        if (parsed == null || parsed < 1 || parsed > 4) {
            return null;
        }
        return parsed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimOrEmpty(List<String> cols, int index) {
        if (index < 0 || index >= cols.size() || cols.get(index) == null) {
            return "";
        }
        return cols.get(index).trim();
    }

    private String trimOrNull(List<String> cols, int index) {
        String v = trimOrEmpty(cols, index);
        return v.isBlank() ? null : v;
    }

    private Integer parseNullableInt(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int parseInt(String v, int fallback) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return fallback;
        }
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

    public static class CsvImportResult {
        private final int imported;
        private final int skipped;

        public CsvImportResult(int imported, int skipped) {
            this.imported = imported;
            this.skipped = skipped;
        }

        public int getImported() {
            return imported;
        }

        public int getSkipped() {
            return skipped;
        }
    }

    public static class QuizState {
        private final List<QuizQuestion> questions;
        private final List<QuizQuestion> wrongQuestions = new ArrayList<>();
        private final String mode;
        private int index;
        private int correctCount;
        private boolean finished;

        public QuizState(List<QuizQuestion> questions, String mode) {
            this.questions = questions;
            this.mode = mode;
        }

        public QuizQuestion current() {
            return questions.get(index);
        }

        public List<QuizQuestion> getQuestions() {
            return questions;
        }

        public List<QuizQuestion> getWrongQuestions() {
            return wrongQuestions;
        }

        public String getMode() {
            return mode;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getCorrectCount() {
            return correctCount;
        }

        public void setCorrectCount(int correctCount) {
            this.correctCount = correctCount;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }
    }

    public static class QuizQuestion {
        private final Long wordId;
        private final String term;
        private final String correctMeaning;
        private final List<String> options;
        private boolean answered;
        private int selectedIndex = -1;
        private boolean correct;

        public QuizQuestion(Long wordId, String term, String correctMeaning, List<String> options) {
            this.wordId = wordId;
            this.term = term;
            this.correctMeaning = correctMeaning;
            this.options = options;
        }

        public Long getWordId() {
            return wordId;
        }

        public String getTerm() {
            return term;
        }

        public String getCorrectMeaning() {
            return correctMeaning;
        }

        public List<String> getOptions() {
            return options;
        }

        public boolean isAnswered() {
            return answered;
        }

        public void setAnswered(boolean answered) {
            this.answered = answered;
        }

        public int getSelectedIndex() {
            return selectedIndex;
        }

        public void setSelectedIndex(int selectedIndex) {
            this.selectedIndex = selectedIndex;
        }

        public boolean isCorrect() {
            return correct;
        }

        public void setCorrect(boolean correct) {
            this.correct = correct;
        }
    }
}
