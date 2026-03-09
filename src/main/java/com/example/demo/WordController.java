package com.example.demo;

import com.example.demo.model.Word;
import com.example.demo.service.WordService;
import com.example.demo.service.WordService.CsvImportResult;
import com.example.demo.service.WordService.QuizQuestion;
import com.example.demo.service.WordService.QuizState;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/words")
public class WordController {

    private static final String QUIZ_STATE = "quizState";
    private static final String REVIEW_QUIZ_STATE = "reviewQuizState";
    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "minorName", required = false) String minorName,
                       @RequestParam(value = "status", required = false, defaultValue = "all") String status,
                       @RequestParam(value = "importance", required = false, defaultValue = "all") String importance,
                       Model model) {
        List<Word> words = wordService.findAll(keyword, minorName, status, importance);
        List<String> minorNames = new ArrayList<>(wordService.findMinorNames());
        model.addAttribute("words", words);
        model.addAttribute("minorNames", minorNames);
        model.addAttribute("keyword", keyword);
        model.addAttribute("minorName", minorName);
        model.addAttribute("status", status);
        model.addAttribute("importance", importance);
        return "words/list";
    }

    @GetMapping("/import/sample")
    public ResponseEntity<byte[]> downloadSampleCsv() {
        byte[] csv = wordService.sampleCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=words-sample.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Word word = wordService.findById(id);
        if (word == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "対象の単語が見つかりませんでした。");
            return "redirect:/words";
        }
        List<String> minorNames = wordService.findMinorNames();
        if (word.getMinorName() != null && minorNames.stream().noneMatch(word.getMinorName()::equals)) {
            minorNames.add(word.getMinorName());
        }
        model.addAttribute("word", word);
        model.addAttribute("minorNames", minorNames);
        return "words/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @RequestParam("term") String term,
                         @RequestParam("meaning") String meaning,
                         @RequestParam("minorName") String minorName,
                         @RequestParam("status") String status,
                         @RequestParam(value = "wrongCount", required = false, defaultValue = "0") Integer wrongCount,
                         @RequestParam(value = "quizEnabled", required = false) String quizEnabled,
                         RedirectAttributes redirectAttributes) {
        Word current = wordService.findById(id);
        if (current == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "対象の単語が見つかりませんでした。");
            return "redirect:/words";
        }
        current.setTerm(term);
        current.setMeaning(meaning);
        current.setMinorName(minorName);
        current.setStatus(status);
        current.setWrongCount(Objects.requireNonNullElse(wrongCount, 0));
        current.setQuizEnabled(quizEnabled != null);

        boolean updated = wordService.updateById(current);
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "単語を更新しました。" : "単語の更新に失敗しました。");
        return updated ? "redirect:/words" : "redirect:/words/" + id + "/edit";
    }

    @PostMapping("/import")
    public String importWords(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            CsvImportResult result = wordService.importCsv(file);
            redirectAttributes.addFlashAttribute("successMessage",
                    result.getImported() + "件取り込み、" + result.getSkipped() + "件スキップしました。");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "単語CSVの取り込みに失敗しました。");
        }
        return "redirect:/words";
    }

    @PostMapping("/quiz-exclude")
    public String excludeFromQuiz(@RequestParam(value = "selectedWordIds", required = false) List<Long> selectedWordIds,
                                  @RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "minorName", required = false) String minorName,
                                  @RequestParam(value = "status", required = false, defaultValue = "all") String status,
                                  @RequestParam(value = "importance", required = false, defaultValue = "all") String importance,
                                  RedirectAttributes redirectAttributes) {
        int updated = wordService.excludeFromQuiz(selectedWordIds);
        if (updated > 0) {
            redirectAttributes.addFlashAttribute("successMessage", updated + "件をクイズ対象外にしました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "対象外にする単語を選択してください。");
        }
        return "redirect:" + buildListQuery(keyword, minorName, status, importance);
    }

    @PostMapping("/{id}/quiz-enabled")
    public String updateQuizEnabled(@PathVariable("id") Long id,
                                    @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                                    @RequestParam(value = "keyword", required = false) String keyword,
                                    @RequestParam(value = "minorName", required = false) String minorName,
                                    @RequestParam(value = "status", required = false, defaultValue = "all") String status,
                                    @RequestParam(value = "importance", required = false, defaultValue = "all") String importance) {
        wordService.updateQuizEnabled(id, enabled);
        return "redirect:" + buildListQuery(keyword, minorName, status, importance);
    }

    @GetMapping("/quiz")
    public String startQuiz(@RequestParam(value = "minorName", required = false) String minorName,
                            HttpSession session,
                            Model model) {
        QuizState state = wordService.createQuiz(minorName);
        session.setAttribute(QUIZ_STATE, state);
        model.addAttribute("minorName", minorName);
        return renderQuiz(model, state);
    }

    @PostMapping("/quiz/answer")
    public String answer(@RequestParam("choiceIndex") int choiceIndex,
                         @RequestParam(value = "minorName", required = false) String minorName,
                         @RequestParam(value = "mode", required = false, defaultValue = WordService.MODE_NORMAL) String mode,
                         HttpSession session,
                         Model model) {
        QuizState state = stateByMode(session, mode);
        if (state == null || state.getQuestions().isEmpty()) {
            return redirectByMode(mode, minorName);
        }
        wordService.answer(state, choiceIndex);
        model.addAttribute("minorName", minorName);
        return renderQuiz(model, state);
    }

    @PostMapping("/quiz/next")
    public String next(@RequestParam(value = "minorName", required = false) String minorName,
                       @RequestParam(value = "mode", required = false, defaultValue = WordService.MODE_NORMAL) String mode,
                       HttpSession session) {
        QuizState state = stateByMode(session, mode);
        if (state == null || state.getQuestions().isEmpty()) {
            return redirectByMode(mode, minorName);
        }
        wordService.next(state);
        if (state.isFinished()) {
            return "redirect:/words/quiz/result?mode=" + mode;
        }
        return "redirect:/words/quiz/current?mode=" + mode + buildMinorQuery(minorName, true);
    }

    @GetMapping("/quiz/current")
    public String current(@RequestParam(value = "minorName", required = false) String minorName,
                          @RequestParam(value = "mode", required = false, defaultValue = WordService.MODE_NORMAL) String mode,
                          HttpSession session,
                          Model model) {
        QuizState state = stateByMode(session, mode);
        if (state == null || state.getQuestions().isEmpty()) {
            return redirectByMode(mode, minorName);
        }
        model.addAttribute("minorName", minorName);
        return renderQuiz(model, state);
    }

    @GetMapping("/quiz/result")
    public String result(@RequestParam(value = "mode", required = false, defaultValue = WordService.MODE_NORMAL) String mode,
                         HttpSession session,
                         Model model) {
        QuizState state = stateByMode(session, mode);
        if (state == null) {
            return "redirect:/words/quiz";
        }
        int total = state.getQuestions().size();
        int correct = state.getCorrectCount();
        int rate = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
        String resultTier = resolveResultTier(correct);
        model.addAttribute("total", total);
        model.addAttribute("correct", correct);
        model.addAttribute("rate", rate);
        model.addAttribute("resultTier", resultTier);
        model.addAttribute("wrongQuestions", state.getWrongQuestions());
        model.addAttribute("mode", mode);
        return "words/quiz-result";
    }

    @PostMapping("/quiz/retry-wrong")
    public String retryWrongOnly(HttpSession session) {
        QuizState state = (QuizState) session.getAttribute(QUIZ_STATE);
        if (state == null || state.getWrongQuestions().isEmpty()) {
            return "redirect:/words/quiz";
        }
        QuizState reviewState = wordService.createQuizFromWrongQuestions(state.getWrongQuestions());
        session.setAttribute(REVIEW_QUIZ_STATE, reviewState);
        return "redirect:/words/quiz/current?mode=" + WordService.MODE_REVIEW;
    }

    private QuizState stateByMode(HttpSession session, String mode) {
        if (WordService.MODE_REVIEW.equals(mode)) {
            return (QuizState) session.getAttribute(REVIEW_QUIZ_STATE);
        }
        return (QuizState) session.getAttribute(QUIZ_STATE);
    }

    private String renderQuiz(Model model, QuizState state) {
        QuizQuestion q = state.current();
        model.addAttribute("current", q);
        model.addAttribute("index", state.getIndex() + 1);
        model.addAttribute("total", state.getQuestions().size());
        model.addAttribute("progressPercent", progressPercent(state));
        model.addAttribute("isAnswered", q.isAnswered());
        model.addAttribute("selectedChoice", q.getSelectedIndex());
        model.addAttribute("isCorrect", q.isCorrect());
        model.addAttribute("correctMeaning", q.getCorrectMeaning());
        model.addAttribute("explanation", q.getCorrectMeaning());
        model.addAttribute("mode", state.getMode());
        return "words/quiz";
    }

    private String redirectByMode(String mode, String minorName) {
        if (WordService.MODE_REVIEW.equals(mode)) {
            return "redirect:/words/quiz/current?mode=" + WordService.MODE_REVIEW;
        }
        return "redirect:/words/quiz" + buildMinorQuery(minorName, false);
    }

    private String buildMinorQuery(String minorName, boolean withPrefixAnd) {
        if (minorName == null || minorName.isBlank()) {
            return "";
        }
        return (withPrefixAnd ? "&" : "?") + "minorName=" + minorName;
    }

    private String buildListQuery(String keyword, String minorName, String status, String importance) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/words");
        if (StringUtils.hasText(keyword)) {
            builder.queryParam("keyword", keyword);
        }
        if (StringUtils.hasText(minorName)) {
            builder.queryParam("minorName", minorName);
        }
        if (StringUtils.hasText(status) && !"all".equals(status)) {
            builder.queryParam("status", status);
        }
        if (StringUtils.hasText(importance) && !"all".equals(importance)) {
            builder.queryParam("importance", importance);
        }
        return builder.build().encode().toUriString();
    }

    private int progressPercent(QuizState state) {
        int total = state.getQuestions().size();
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(((state.getIndex() + 1) * 100.0) / total);
    }

    private String resolveResultTier(int correct) {
        if (correct >= 10) {
            return "PERFECT";
        }
        if (correct >= 8) {
            return "HIGH";
        }
        if (correct >= 5) {
            return "MID";
        }
        return "LOW";
    }
}
