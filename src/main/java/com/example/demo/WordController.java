package com.example.demo;

import com.example.demo.model.Word;
import com.example.demo.service.WordService;
import com.example.demo.service.WordService.QuizQuestion;
import com.example.demo.service.WordService.QuizState;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/words")
public class WordController {

    private static final String QUIZ_STATE = "quizState";
    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "minorName", required = false) String minorName,
                       @RequestParam(value = "status", required = false, defaultValue = "all") String status,
                       Model model) {
        List<Word> words = wordService.findAll(keyword, minorName, status);
        model.addAttribute("words", words);
        model.addAttribute("keyword", keyword);
        model.addAttribute("minorName", minorName);
        model.addAttribute("status", status);
        return "words/list";
    }

    @PostMapping("/import")
    public String importWords(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            int count = wordService.importCsv(file);
            redirectAttributes.addFlashAttribute("successMessage", count + "件の単語を取り込みました。");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "単語CSVの取り込みに失敗しました。");
        }
        return "redirect:/words";
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
                         HttpSession session,
                         Model model) {
        QuizState state = (QuizState) session.getAttribute(QUIZ_STATE);
        if (state == null || state.getQuestions().isEmpty()) {
            return "redirect:/words/quiz" + buildMinorQuery(minorName);
        }
        wordService.answer(state, choiceIndex);
        model.addAttribute("minorName", minorName);
        return renderQuiz(model, state);
    }

    @PostMapping("/quiz/next")
    public String next(@RequestParam(value = "minorName", required = false) String minorName,
                       HttpSession session) {
        QuizState state = (QuizState) session.getAttribute(QUIZ_STATE);
        if (state == null || state.getQuestions().isEmpty()) {
            return "redirect:/words/quiz" + buildMinorQuery(minorName);
        }
        wordService.next(state);
        if (state.isFinished()) {
            return "redirect:/words/quiz/result";
        }
        return "redirect:/words/quiz/current" + buildMinorQuery(minorName);
    }

    @GetMapping("/quiz/current")
    public String current(@RequestParam(value = "minorName", required = false) String minorName,
                          HttpSession session,
                          Model model) {
        QuizState state = (QuizState) session.getAttribute(QUIZ_STATE);
        if (state == null || state.getQuestions().isEmpty()) {
            return "redirect:/words/quiz" + buildMinorQuery(minorName);
        }
        model.addAttribute("minorName", minorName);
        return renderQuiz(model, state);
    }

    @GetMapping("/quiz/result")
    public String result(HttpSession session, Model model) {
        QuizState state = (QuizState) session.getAttribute(QUIZ_STATE);
        if (state == null) {
            return "redirect:/words/quiz";
        }
        int total = state.getQuestions().size();
        int correct = state.getCorrectCount();
        int rate = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
        model.addAttribute("total", total);
        model.addAttribute("correct", correct);
        model.addAttribute("rate", rate);
        model.addAttribute("wrongQuestions", state.getWrongQuestions());
        return "words/quiz-result";
    }

    private String renderQuiz(Model model, QuizState state) {
        QuizQuestion q = state.current();
        model.addAttribute("current", q);
        model.addAttribute("index", state.getIndex() + 1);
        model.addAttribute("total", state.getQuestions().size());
        model.addAttribute("isAnswered", q.isAnswered());
        return "words/quiz";
    }

    private String buildMinorQuery(String minorName) {
        if (minorName == null || minorName.isBlank()) {
            return "";
        }
        return "?minorName=" + minorName;
    }
}
