package com.jep.servidor.controller.frontend;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador para a área administrativa.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    @Autowired
    private UserRelationRepository relationRepository;

    private boolean isAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return false;
        }

        // Verificar no banco de dados para garantir que a permissão é atual
        return userRepository.findById(userId)
                .map(u -> u.getUserType() == User.UserType.USERADMIN)
                .orElse(false);
    }

    /**
     * Exibe o dashboard administrativo.
     *
     * @param search Termo de pesquisa opcional.
     * @param session Sessão HTTP.
     * @param model Modelo para a view.
     * @return Nome da view do dashboard.
     */
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "search", required = false) String search,
            HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<User> users;
        if (search != null && !search.isEmpty()) {
            users = userRepository
                    .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        } else {
            users = userRepository.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("userRepository", userRepository);
        model.addAttribute("podcasts", podcastRepository.findAll());
        model.addAttribute("relations", relationRepository.findAll());
        return "admin_dashboard";
    }

    /**
     * Atualiza o estado de um utilizador.
     *
     * @param id ID do utilizador.
     * @param status Novo estado.
     * @param session Sessão HTTP.
     * @return Redirecionamento para o dashboard.
     */
    @PostMapping("/user/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
            @RequestParam("status") User.UserStatus status,
            HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        userRepository.findById(id).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
        return "redirect:/admin/dashboard";
    }

    /**
     * Redefine a palavra-passe de um utilizador.
     *
     * @param id ID do utilizador.
     * @param session Sessão HTTP.
     * @return Redirecionamento para o dashboard.
     */
    @PostMapping("/user/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        userRepository.findById(id).ifPresent(user -> {
            user.setPassword("1234"); // Password padrão de reset
            userRepository.save(user);
        });
        return "redirect:/admin/dashboard";
    }
}
