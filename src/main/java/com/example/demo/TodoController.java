package com.example.demo;

import com.example.demo.form.TodoForm;
import com.example.demo.model.Priority;
import com.example.demo.model.Subject;
import com.example.demo.model.Todo;
import com.example.demo.service.SubjectService;
import com.example.demo.service.TodoService;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/todo")
public class TodoController {

    private static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TodoService todoService;
    private final SubjectService subjectService;

    public TodoController(TodoService todoService, SubjectService subjectService) {
        this.todoService = todoService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "subjectId", required = false) Long subjectId,
                       @RequestParam(value = "sort", required = false, defaultValue = "createdAt") String sort,
                       @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction,
                       @PageableDefault(size = 10) Pageable pageable,
                       Model model) {
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        Page<Todo> todoPage = todoService.findPage(keyword, subjectId, normalizedSort, normalizedDirection, pageable);
        long totalElements = todoPage.getTotalElements();
        long start = totalElements == 0 ? 0 : pageable.getOffset() + 1;
        long end = totalElements == 0 ? 0 : Math.min(pageable.getOffset() + pageable.getPageSize(), totalElements);

        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("direction", normalizedDirection);
        model.addAttribute("currentPage", todoPage.getNumber());
        model.addAttribute("totalPages", todoPage.getTotalPages());
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("startItem", start);
        model.addAttribute("endItem", end);
        model.addAttribute("todos", todoPage.getContent());
        return "todo/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("todoForm")) {
            TodoForm todoForm = new TodoForm();
            List<Subject> subjects = subjectService.findAll();
            if (!subjects.isEmpty()) {
                todoForm.setSubjectId(subjects.get(0).getId());
            }
            model.addAttribute("todoForm", todoForm);
        }
        model.addAttribute("subjects", subjectService.findAll());
        return "todo/form";
    }

    @PostMapping("/confirm")
    public String confirm(@Valid @ModelAttribute("todoForm") TodoForm todoForm,
                          BindingResult bindingResult,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("subjects", subjectService.findAll());
            return "todo/form";
        }
        model.addAttribute("pageTitle", "登録内容の確認");
        model.addAttribute("registerLabel", "登録");
        model.addAttribute("backLabel", "戻る");
        model.addAttribute("selectedSubject", subjectService.findById(todoForm.getSubjectId()));
        return "todo/confirm";
    }

    @PostMapping("/complete")
    public String complete(@ModelAttribute("todoForm") TodoForm todoForm) {
        Todo todo = new Todo();
        todo.setTitle(todoForm.getTitle());
        todo.setCompleted(Boolean.FALSE);
        todo.setPriority(todoForm.getPriority() != null ? todoForm.getPriority() : Priority.MEDIUM);
        Subject subject = new Subject();
        subject.setId(todoForm.getSubjectId());
        todo.setSubject(subject);
        todo.setDeadline(todoForm.getDeadline());
        todoService.insert(todo);
        return "redirect:/todo";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "ToDoが見つかりません");
            return "redirect:/todo";
        }
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("todo", todo);
        return "todo/edit";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @RequestParam("title") String title,
                         @RequestParam("priority") Priority priority,
                         @RequestParam("subjectId") Long subjectId,
                         @RequestParam(value = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                         RedirectAttributes redirectAttributes) {
        Todo existing = todoService.findById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新に失敗しました");
            return "redirect:/todo";
        }

        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(existing.getCompleted() != null ? existing.getCompleted() : Boolean.FALSE);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        Subject subject = new Subject();
        subject.setId(subjectId);
        todo.setSubject(subject);
        todo.setDeadline(deadline);

        boolean updated = todoService.update(todo);
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "更新が完了しました" : "更新に失敗しました");
        return "redirect:/todo";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = todoService.deleteById(id);
        redirectAttributes.addFlashAttribute(deleted ? "successMessage" : "errorMessage",
                deleted ? "ToDoを削除しました" : "削除に失敗しました");
        return "redirect:/todo";
    }

    @PostMapping("/{id}/delete")
    public String deleteLegacy(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        return delete(id, redirectAttributes);
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(value = "ids", required = false) List<Integer> ids,
                             RedirectAttributes redirectAttributes) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除するToDoを選択してください");
            return "redirect:/todo";
        }
        List<Long> targetIds = ids.stream().map(Integer::longValue).toList();
        int deletedCount = todoService.deleteByIds(targetIds);
        redirectAttributes.addFlashAttribute(deletedCount > 0 ? "successMessage" : "errorMessage",
                deletedCount > 0 ? deletedCount + "件のToDoを削除しました" : "削除に失敗しました");
        return "redirect:/todo";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable("id") Long id) {
        todoService.toggleCompleted(id);
        return "redirect:/todo";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        List<Todo> todos = todoService.findAllForExport();
        String csv = buildCsv(todos);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        out.writeBytes(csv.getBytes(StandardCharsets.UTF_8));

        String filename = "todo_" + LocalDate.now().format(FILE_DATE) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return ResponseEntity.ok().headers(headers).body(out.toByteArray());
    }

    private String normalizeSort(String sort) {
        return switch (sort) {
            case "title", "completed", "priority", "deadline" -> sort;
            default -> "createdAt";
        };
    }

    private String normalizeDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }

    private String buildCsv(List<Todo> todos) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,タイトル,分野,ステータス,作成日").append("\r\n");
        for (Todo todo : todos) {
            String id = todo.getId() != null ? String.valueOf(todo.getId()) : "";
            String title = todo.getTitle() != null ? todo.getTitle() : "";
            String subjectName = todo.getSubject() != null
                    ? formatSubject(todo.getSubject().getFieldName(), todo.getSubject().getMajorName(), todo.getSubject().getMinorName())
                    : "-";
            String status = Boolean.TRUE.equals(todo.getCompleted()) ? "完了" : "未完了";
            String createdAt = todo.getCreatedAt() != null ? todo.getCreatedAt().format(CSV_DATE_TIME) : "";
            sb.append(escapeCsv(id)).append(',')
                    .append(escapeCsv(title)).append(',')
                    .append(escapeCsv(subjectName)).append(',')
                    .append(escapeCsv(status)).append(',')
                    .append(escapeCsv(createdAt))
                    .append("\r\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String formatSubject(String fieldName, String majorName, String minorName) {
        return String.join(" / ",
                fieldName != null ? fieldName : "-",
                majorName != null ? majorName : "-",
                minorName != null ? minorName : "-");
    }
}
