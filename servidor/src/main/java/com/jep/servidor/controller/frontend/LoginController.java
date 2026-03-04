package com.jep.servidor.controller.frontend;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/profile";
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("loginType") String loginType,
            @RequestParam("identifier") String identifier,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        Optional<User> userOpt;

        if ("email".equalsIgnoreCase(loginType)) {
            userOpt = userRepository.findByEmail(identifier);
        } else {
            // Se for login por username + tag
            userOpt = userRepository.findByUsernameAndTag(identifier, tag);
        }

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            User user = userOpt.get();
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                model.addAttribute("error", "A sua conta está " +
                        (user.getStatus() == User.UserStatus.BANNED ? "BANIDA" : "SUSPENSA") +
                        ". Contacte o suporte técnico.");
                return "login";
            }
            session.setAttribute("userId", user.getId());
            session.setAttribute("userType", user.getUserType().toString());

            // Atualizar data de último acesso
            user.setLastActiveAt(java.time.LocalDateTime.now());
            userRepository.save(user);

            return "redirect:/profile";
        } else {
            model.addAttribute("error", "Utilizador ou password incorretos.");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
