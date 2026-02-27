package com.example.demo;

import java.util.List;

import com.example.demo.form.TodoForm;
import com.example.demo.model.Todo;
import jakarta.validation.Valid;
import com.example.demo.service.TodoService;
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

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "sort", required = false, defaultValue = "createdAt") String sort,
                       @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction,
                       Model model) {
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        List<Todo> todos;
        if (keyword != null && !keyword.isBlank()) {
            todos = todoService.searchByTitle(keyword, normalizedSort, normalizedDirection);
        } else {
            todos = todoService.findAll(normalizedSort, normalizedDirection);
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("direction", normalizedDirection);
        model.addAttribute("nextDirection", "asc".equals(normalizedDirection) ? "desc" : "asc");
        model.addAttribute("resultCount", todos.size());
        model.addAttribute("todos", todos);
        return "todo/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("todoForm")) {
            model.addAttribute("todoForm", new TodoForm());
        }
        return "todo/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "\u0054\u006f\u0044\u006f\u304c\u898b\u3064\u304b\u308a\u307e\u305b\u3093");
            return "redirect:/todo";
        }
        model.addAttribute("todo", todo);
        return "todo/edit";
    }

    @PostMapping("/confirm")
    public String confirm(@Valid @ModelAttribute("todoForm") TodoForm todoForm,
                          BindingResult bindingResult,
                          Model model) {
        if (bindingResult.hasErrors()) {
            return "todo/form";
        }
        model.addAttribute("pageTitle", "\u767B\u9332\u5185\u5BB9\u306E\u78BA\u8A8D");
        model.addAttribute("registerLabel", "\u767B\u9332");
        model.addAttribute("backLabel", "\u623B\u308B");
        return "todo/confirm";
    }

    @PostMapping("/complete")
    public String complete(@ModelAttribute("todoForm") TodoForm todoForm) {
        Todo todo = new Todo();
        todo.setTitle(todoForm.getTitle());
        todo.setCompleted(Boolean.FALSE);
        todoService.insert(todo);
        return "redirect:/todo";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @RequestParam("title") String title,
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

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable("id") Long id) {
        todoService.toggleCompleted(id);
        return "redirect:/todo";
    }

    private String normalizeSort(String sort) {
        return switch (sort) {
            case "title", "completed" -> sort;
            default -> "createdAt";
        };
    }

    private String normalizeDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }
}
