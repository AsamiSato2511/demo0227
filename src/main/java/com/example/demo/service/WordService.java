package com.example.demo.service;

import com.example.demo.mapper.WordMapper;
import com.example.demo.model.Word;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WordService {

    private static final String STATUS_LEARNING = "学習中";
    private static final String STATUS_REVIEW = "要復習";
    private final WordMapper wordMapper;
    private final Random random = new Random();

    public WordService(WordMapper wordMapper) {
        this.wordMapper = wordMapper;
    }

    public List<Word> findAll(String keyword, String minorName, String status) {
        return wordMapper.findAll(keyword, minorName, status);
    }

    public int importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return 0;
        }
        int imported = 0;
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
                if (cols.size() < 3) {
                    continue;
                }
                Word w = new Word();
                w.setTerm(cols.get(0).trim());
                w.setMeaning(cols.get(1).trim());
                w.setMinorName(cols.get(2).trim());
                w.setStatus(cols.size() > 3 && !cols.get(3).isBlank() ? cols.get(3).trim() : STATUS_LEARNING);
                w.setWrongCount(cols.size() > 4 && !cols.get(4).isBlank() ? parseInt(cols.get(4).trim(), 0) : 0);
                w.setFieldName("未分類");
                w.setMajorName("未分類");
                if (w.getTerm().isBlank() || w.getMeaning().isBlank() || w.getMinorName().isBlank()) {
                    continue;
                }
                wordMapper.upsert(w);
                imported++;
            }
        }
        return imported;
    }

    public QuizState createQuiz(String minorName) {
        List<Word> pool = wordMapper.findAllPool(minorName);
        if (pool.isEmpty()) {
            return new QuizState(Collections.emptyList());
        }

        List<Word> sorted = new ArrayList<>(pool);
        Collections.shuffle(sorted, random);
        sorted.sort(Comparator.comparing((Word w) -> w.getWrongCount() != null ? w.getWrongCount() : 0).reversed());

        int count = Math.min(10, sorted.size());
        List<QuizQuestion> questions = new ArrayList<>();
        List<Word> allPool = wordMapper.findAllPool(null);
        for (int i = 0; i < count; i++) {
            Word target = sorted.get(i);
            List<String> options = buildOptions(target, allPool);
            questions.add(new QuizQuestion(target.getId(), target.getTerm(), target.getMeaning(), options));
        }
        return new QuizState(questions);
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
        } else {
            state.getWrongQuestions().add(q);
            wordMapper.incrementWrongCount(q.getWordId());
        }
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

    private List<String> buildOptions(Word target, List<Word> allPool) {
        Set<String> used = new HashSet<>();
        List<String> options = new ArrayList<>();
        options.add(target.getMeaning());
        used.add(target.getMeaning());

        List<Word> sameMinor = new ArrayList<>();
        for (Word w : allPool) {
            if (w.getMinorName() != null && w.getMinorName().equals(target.getMinorName())) {
                sameMinor.add(w);
            }
        }
        Collections.shuffle(sameMinor, random);
        for (Word cand : sameMinor) {
            if (options.size() >= 4) {
                break;
            }
            if (cand.getId().equals(target.getId()) || used.contains(cand.getMeaning())) {
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
                if (cand.getId().equals(target.getId()) || used.contains(cand.getMeaning())) {
                    continue;
                }
                options.add(cand.getMeaning());
                used.add(cand.getMeaning());
            }
        }
        Collections.shuffle(options, random);
        return options;
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

    public static class QuizState {
        private final List<QuizQuestion> questions;
        private final List<QuizQuestion> wrongQuestions = new ArrayList<>();
        private int index;
        private int correctCount;
        private boolean finished;

        public QuizState(List<QuizQuestion> questions) {
            this.questions = questions;
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
