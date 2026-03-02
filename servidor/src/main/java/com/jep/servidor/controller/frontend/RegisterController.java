package com.jep.servidor.controller.frontend;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String tag,
                               @RequestParam String email,
                               @RequestParam String password,
                               Model model) {
        if (username.isEmpty() || tag.isEmpty() || email.isEmpty() || password.isEmpty() || tag.length() != 4) {
            model.addAttribute("error", "Preencha todos os campos corretamente.");
            return "register";
        }
        if (userRepository.existsByUsernameAndTag(username, tag)) {
            model.addAttribute("error", "Tag já ocupada para este username.");
            return "register";
        }
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email já está em uso.");
            return "register";
        }
        User user = new User();
        user.setUsername(username);
        user.setTag(tag);
        user.setEmail(email);
        user.setPassword(password);
        user.setUserType(User.UserType.USERNORMAL);
        userRepository.save(user);
        model.addAttribute("success", "Conta criada com sucesso!");
        return "register";
    }

    @GetMapping("/register/check-tag")
    @ResponseBody
    public String checkTag(@RequestParam String username, @RequestParam String tag) {
        if (tag.length() != 4) return "Tag inválida";
        boolean exists = userRepository.existsByUsernameAndTag(username, tag);
        return exists ? "Tag ocupada" : "Tag disponível";
    }

    @GetMapping("/register/generate-tag")
    @ResponseBody
    public String generateTag(@RequestParam String username) {
        for (int i = 0; i <= 9999; i++) {
            String tag = String.format("%04d", i);
            if (!userRepository.existsByUsernameAndTag(username, tag)) {
                return tag;
            }
        }
        return "Nenhuma tag disponível";
    }
}
