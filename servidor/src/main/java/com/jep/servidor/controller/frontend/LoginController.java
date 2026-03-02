package com.jep.servidor.controller.frontend;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
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
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String loginType,
            @RequestParam String identifier,
            @RequestParam(required = false) String tag,
            @RequestParam String password,
            Model model) {

        Optional<User> userOpt;

        if ("email".equalsIgnoreCase(loginType)) {
            userOpt = userRepository.findByEmail(identifier);
        } else {
            // Se for login por username + tag
            userOpt = userRepository.findByUsernameAndTag(identifier, tag);
        }

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            model.addAttribute("success",
                    "Login efetuado com sucesso! Bem-vindo(a), " + userOpt.get().getUsername() + ".");
            // No futuro, aqui poderíamos adicionar o utilizador à sessão (HttpSession)
            return "login";
        } else {
            model.addAttribute("error", "Utilizador ou password incorretos.");
            return "login";
        }
    }
}
