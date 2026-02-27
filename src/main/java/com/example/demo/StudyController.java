package com.example.demo;

import com.example.demo.form.ExamSettingForm;
import com.example.demo.model.ExamSetting;
import com.example.demo.service.StudyService;
import com.example.demo.service.SubjectService;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/study")
public class StudyController {

    private final StudyService studyService;
    private final SubjectService subjectService;

    public StudyController(StudyService studyService, SubjectService subjectService) {
        this.studyService = studyService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String index(@RequestParam(value = "period", required = false, defaultValue = "all") String period,
                        Model model) {
        ExamSetting examSetting = studyService.getExamSetting();
        LocalDate examDate = examSetting != null ? examSetting.getExamDate() : null;
        long days = studyService.getDaysUntilExam(examDate);

        ExamSettingForm examSettingForm = new ExamSettingForm();
        examSettingForm.setExamDate(examDate);

        model.addAttribute("examDate", examDate);
        model.addAttribute("countdownMessage", studyService.buildCountdownMessage(days));
        model.addAttribute("examSettingForm", examSettingForm);
        model.addAttribute("period", period);
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("averageUnderstanding", subjectService.findAverageUnderstanding());
        model.addAttribute("fieldRates", studyService.findFieldRates(period));
        model.addAttribute("minorRates", studyService.findMinorRates(period));
        model.addAttribute("worstMinorRates", studyService.findWorstMinorRates(period, 5));
        model.addAttribute("recentAttempts", studyService.findRecentAttempts(period, 20));
        return "study/index";
    }

    @PostMapping("/exam-date")
    public String updateExamDate(@ModelAttribute ExamSettingForm examSettingForm, RedirectAttributes redirectAttributes) {
        boolean updated = studyService.updateExamDate(examSettingForm.getExamDate());
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "試験日を更新しました" : "試験日の更新に失敗しました");
        return "redirect:/study";
    }

    @PostMapping("/subjects/{id}/understanding")
    public String updateUnderstanding(@PathVariable("id") Long id,
                                      @RequestParam("understanding") Integer understanding,
                                      RedirectAttributes redirectAttributes) {
        boolean updated = subjectService.updateUnderstanding(id, understanding);
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "理解度を更新しました" : "理解度の更新に失敗しました");
        return "redirect:/study";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            int imported = studyService.importQuestionAttempts(file);
            if (imported > 0) {
                redirectAttributes.addFlashAttribute("successMessage", imported + "件を取り込みました");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "取り込めるデータがありませんでした");
            }
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "CSVの取り込みに失敗しました");
        }
        return "redirect:/study";
    }
}
