package com.jep.servidor.controller.frontend;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.jep.servidor.repository.PodcastRepository podcastRepository;

    @Autowired
    private com.jep.servidor.repository.UserRelationRepository relationRepository;

    private boolean isAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return false;

        // Verificar no banco de dados para garantir que a permissão é atual
        return userRepository.findById(userId)
                .map(u -> u.getUserType() == User.UserType.USERADMIN)
                .orElse(false);
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "search", required = false) String search,
            HttpSession session, Model model) {
        if (!isAdmin(session))
            return "redirect:/login";

        List<User> users;
        if (search != null && !search.isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        } else {
            users = userRepository.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("userRepository", userRepository);
        model.addAttribute("podcasts", podcastRepository.findAll());
        model.addAttribute("relations", relationRepository.findAll());
        return "admin_dashboard";
    }

    @PostMapping("/user/{id}/status")
    public String updateStatus(@PathVariable("id") Long id, @RequestParam("status") User.UserStatus status,
            HttpSession session) {
        if (!isAdmin(session))
            return "redirect:/login";

        userRepository.findById(id).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/user/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id, HttpSession session) {
        if (!isAdmin(session))
            return "redirect:/login";

        userRepository.findById(id).ifPresent(user -> {
            user.setPassword("1234"); // Password padrão de reset
            userRepository.save(user);
        });
        return "redirect:/admin/dashboard";
    }
}
