package com.example.demo;

import com.example.demo.form.ExamSettingForm;
import com.example.demo.model.CorrectRateSummary;
import com.example.demo.model.ExamResult;
import com.example.demo.model.ExamSetting;
import com.example.demo.service.FieldImpact;
import com.example.demo.service.PassForecast;
import com.example.demo.service.AdviceService;
import com.example.demo.service.StudyService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/study")
public class StudyController {

    private final StudyService studyService;
    private final AdviceService adviceService;

    public StudyController(StudyService studyService, AdviceService adviceService) {
        this.studyService = studyService;
        this.adviceService = adviceService;
    }

    @GetMapping
    public String index(@RequestParam(value = "batch", required = false, defaultValue = "all") String batch,
                        Model model) {
        ExamSetting examSetting = studyService.getExamSetting();
        LocalDate examDate = examSetting != null ? examSetting.getExamDate() : null;
        long daysUntilExam = studyService.getDaysUntilExam(examDate);

        List<ExamResult> recentResults = studyService.findRecentResults(20);
        int latestScore = studyService.latestScore(recentResults);
        int remainToPass = Math.max(0, StudyService.PASS_LINE - latestScore);
        int previousDiff = studyService.previousDiff(recentResults);
        PassForecast forecast = studyService.calculatePassForecast(recentResults);
        int progressPercent = Math.min(100, latestScore * 100 / StudyService.PASS_LINE);
        String progressColor = latestScore < 50 ? "#dc3545" : (latestScore < StudyService.PASS_LINE ? "#ffc107" : "#198754");

        List<CorrectRateSummary> fieldRates = studyService.findFieldRates(batch);
        List<CorrectRateSummary> bottleneckTop3 = studyService.findBottleneckFieldsTop3(batch);
        List<FieldImpact> impacts = studyService.calculateFieldImpacts(batch);
        List<String> weakMinorNames = studyService.findWeakMinorNames(3, batch);
        List<String> adviceLines = adviceService.buildAdvice(recentResults, impacts, bottleneckTop3, remainToPass, previousDiff);

        List<ExamResult> trendAsc = new ArrayList<>(recentResults);
        Collections.reverse(trendAsc);
        List<String> trendLabels = new ArrayList<>();
        List<Integer> trendScores = new ArrayList<>();
        for (ExamResult row : trendAsc) {
            trendLabels.add(row.getTakenOn() != null ? row.getTakenOn().toString() : "-");
            trendScores.add(row.getScoreTotal() != null ? row.getScoreTotal() : 0);
        }

        ExamSettingForm examSettingForm = new ExamSettingForm();
        examSettingForm.setExamDate(examDate);

        model.addAttribute("batch", batch);
        model.addAttribute("importBatches", studyService.findImportBatches());
        model.addAttribute("examDate", examDate);
        model.addAttribute("daysUntilExam", daysUntilExam);
        model.addAttribute("countdownMessage", studyService.buildCountdownMessage(daysUntilExam));
        model.addAttribute("examSettingForm", examSettingForm);

        model.addAttribute("latestScore", latestScore);
        model.addAttribute("passLine", StudyService.PASS_LINE);
        model.addAttribute("remainToPass", remainToPass);
        model.addAttribute("previousDiff", previousDiff);
        model.addAttribute("forecast", forecast);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("progressColor", progressColor);

        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendScores", trendScores);
        model.addAttribute("fieldRates", fieldRates);
        model.addAttribute("fieldImpacts", impacts);
        model.addAttribute("bottleneckTop3", bottleneckTop3);
        model.addAttribute("weakMinorNames", weakMinorNames);
        model.addAttribute("adviceLines", adviceLines);
        model.addAttribute("minorRates", studyService.findMinorRates(batch));
        return "study/index";
    }

    @PostMapping("/exam-date")
    public String updateExamDate(@ModelAttribute ExamSettingForm examSettingForm,
                                 RedirectAttributes redirectAttributes) {
        boolean updated = studyService.updateExamDate(examSettingForm.getExamDate());
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "試験日を更新しました。" : "試験日の更新に失敗しました。");
        return "redirect:/study";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        try {
            int imported = studyService.importQuestionAttempts(file);
            if (imported > 0) {
                redirectAttributes.addFlashAttribute("successMessage", imported + "件を取り込みました。");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "取り込める行がありませんでした。");
            }
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "CSV取込に失敗しました。");
        }
        return "redirect:/study";
    }
}
