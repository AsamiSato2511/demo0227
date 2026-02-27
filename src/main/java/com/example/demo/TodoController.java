package com.example.demo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.example.demo.form.TodoForm;
import com.example.demo.model.Category;
import com.example.demo.model.Priority;
import com.example.demo.model.Todo;
import jakarta.validation.Valid;
import com.example.demo.service.CategoryService;
import com.example.demo.service.TodoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final CategoryService categoryService;

    public TodoController(TodoService todoService, CategoryService categoryService) {
        this.todoService = todoService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "categoryId", required = false) Long categoryId,
                       @RequestParam(value = "sort", required = false, defaultValue = "createdAt") String sort,
                       @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction,
                       @PageableDefault(size = 10) Pageable pageable,
                       Model model) {
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        Page<Todo> todoPage = todoService.findPage(keyword, categoryId, normalizedSort, normalizedDirection, pageable);
        List<Todo> todos = todoPage.getContent();
        long totalElements = todoPage.getTotalElements();
        long start = totalElements == 0 ? 0 : pageable.getOffset() + 1;
        long end = totalElements == 0 ? 0 : Math.min(pageable.getOffset() + pageable.getPageSize(), totalElements);

        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("direction", normalizedDirection);
        model.addAttribute("page", todoPage);
        model.addAttribute("currentPage", todoPage.getNumber());
        model.addAttribute("totalPages", todoPage.getTotalPages());
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("startItem", start);
        model.addAttribute("endItem", end);
        model.addAttribute("todos", todos);
        return "todo/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("todoForm")) {
            TodoForm todoForm = new TodoForm();
            List<Category> categories = categoryService.findAll();
            if (!categories.isEmpty()) {
                todoForm.setCategoryId(categories.get(0).getId());
            }
            model.addAttribute("todoForm", todoForm);
        }
        model.addAttribute("categories", categoryService.findAll());
        return "todo/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "\u0054\u006f\u0044\u006f\u304c\u898b\u3064\u304b\u308a\u307e\u305b\u3093");
            return "redirect:/todo";
        }
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("todo", todo);
        return "todo/edit";
    }

    @PostMapping("/confirm")
    public String confirm(@Valid @ModelAttribute("todoForm") TodoForm todoForm,
                          BindingResult bindingResult,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "todo/form";
        }
        model.addAttribute("pageTitle", "\u767B\u9332\u5185\u5BB9\u306E\u78BA\u8A8D");
        model.addAttribute("registerLabel", "\u767B\u9332");
        model.addAttribute("backLabel", "\u623B\u308B");
        model.addAttribute("selectedCategory", categoryService.findById(todoForm.getCategoryId()));
        return "todo/confirm";
    }

    @PostMapping("/complete")
    public String complete(@ModelAttribute("todoForm") TodoForm todoForm) {
        Todo todo = new Todo();
        todo.setTitle(todoForm.getTitle());
        todo.setCompleted(Boolean.FALSE);
        todo.setPriority(todoForm.getPriority() != null ? todoForm.getPriority() : Priority.MEDIUM);
        Category category = new Category();
        category.setId(todoForm.getCategoryId());
        todo.setCategory(category);
        todo.setDeadline(todoForm.getDeadline());
        todoService.insert(todo);
        return "redirect:/todo";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @RequestParam("title") String title,
                         @RequestParam("priority") Priority priority,
                         @RequestParam("categoryId") Long categoryId,
                         @RequestParam(value = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                         RedirectAttributes redirectAttributes) {
        Todo existing = todoService.findById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "\u66F4\u65B0\u306B\u5931\u6557\u3057\u307E\u3057\u305F");
            return "redirect:/todo";
        }

        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(existing.getCompleted() != null ? existing.getCompleted() : Boolean.FALSE);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        Category category = new Category();
        category.setId(categoryId);
        todo.setCategory(category);
        todo.setDeadline(deadline);

        boolean updated = todoService.update(todo);
        if (updated) {
            redirectAttributes.addFlashAttribute("successMessage", "\u66F4\u65B0\u304C\u5B8C\u4E86\u3057\u307E\u3057\u305F");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "\u66F4\u65B0\u306B\u5931\u6557\u3057\u307E\u3057\u305F");
        }
        return "redirect:/todo";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = todoService.deleteById(id);
            if (deleted) {
                redirectAttributes.addFlashAttribute("successMessage", "\u0054\u006f\u0044\u006f\u3092\u524a\u9664\u3057\u307e\u3057\u305f");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "\u524a\u9664\u306b\u5931\u6557\u3057\u307e\u3057\u305f");
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "\u524a\u9664\u306b\u5931\u6557\u3057\u307e\u3057\u305f");
        }
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

        List<Long> targetIds = ids.stream()
                .map(Integer::longValue)
                .toList();
        int deletedCount = todoService.deleteByIds(targetIds);
        if (deletedCount > 0) {
            redirectAttributes.addFlashAttribute("successMessage", deletedCount + "件のToDoを削除しました");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
        }
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

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable("id") Long id) {
        todoService.toggleCompleted(id);
        return "redirect:/todo";
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
        sb.append("ID,タイトル,登録者,ステータス,作成日").append("\r\n");
        for (Todo todo : todos) {
            String id = todo.getId() != null ? String.valueOf(todo.getId()) : "";
            String title = todo.getTitle() != null ? todo.getTitle() : "";
            String author = "-";
            String status = Boolean.TRUE.equals(todo.getCompleted()) ? "完了" : "未完了";
            String createdAt = todo.getCreatedAt() != null ? todo.getCreatedAt().format(CSV_DATE_TIME) : "";
            sb.append(escapeCsv(id)).append(',')
                    .append(escapeCsv(title)).append(',')
                    .append(escapeCsv(author)).append(',')
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
}
