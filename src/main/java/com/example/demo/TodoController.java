package com.example.demo;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/todo")
public class TodoController {

    @GetMapping
    public String list(Model model) {
        List<TodoItemView> todos = List.of(
                new TodoItemView(1L, "Learn Spring Boot", "TODO"),
                new TodoItemView(2L, "Create list page", "DOING"),
                new TodoItemView(3L, "Implement create feature", "TODO"));

        model.addAttribute("todos", todos);
        return "todo/list";
    }

    @GetMapping("/new")
    public String newForm() {
        return "todo/form";
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam("title") String title, Model model) {
        model.addAttribute("pageTitle", "\u767B\u9332\u5185\u5BB9\u306E\u78BA\u8A8D");
        model.addAttribute("todoTitle", title);
        model.addAttribute("registerLabel", "\u767B\u9332");
        model.addAttribute("backLabel", "\u623B\u308B");
        return "todo/confirm";
    }

    @PostMapping("/complete")
    public String complete(@RequestParam("title") String title, Model model) {
        model.addAttribute("completeMessage", "\u767B\u9332\u304C\u5B8C\u4E86\u3057\u307E\u3057\u305F");
        model.addAttribute("todoTitle", title);
        model.addAttribute("backToListLabel", "\u4E00\u89A7\u3078\u623B\u308B");
        return "todo/complete";
    }

    public record TodoItemView(Long id, String title, String status) {
    }
}
