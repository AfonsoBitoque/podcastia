package com.jep.servidor.controller.frontend;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controlador para registo de novos utilizadores.
 */
@Controller
public class RegisterController {
    @Autowired
    private UserRepository userRepository;

    /**
     * Exibe o formulário de registo.
     *
     * @return Nome da view de registo.
     */
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    /**
     * Processa o registo de um novo utilizador.
     *
     * @param username Nome de utilizador.
     * @param tag Tag do utilizador.
     * @param email Email do utilizador.
     * @param password Palavra-passe.
     * @param model Modelo para a view.
     * @return Nome da view de registo com mensagem de sucesso ou erro.
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
            @RequestParam("tag") String tag,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            Model model) {
        if (username.isEmpty() || tag.isEmpty() || email.isEmpty() || password.isEmpty()
                || tag.length() != 4) {
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

    /**
     * Verifica se uma tag está disponível para um determinado username.
     *
     * @param username Nome de utilizador.
     * @param tag Tag a verificar.
     * @return Mensagem indicando se a tag está disponível ou ocupada.
     */
    @GetMapping("/register/check-tag")
    @ResponseBody
    public String checkTag(@RequestParam("username") String username,
            @RequestParam("tag") String tag) {
        if (tag.length() != 4) {
            return "Tag inválida";
        }
        boolean exists = userRepository.existsByUsernameAndTag(username, tag);
        return exists ? "Tag ocupada" : "Tag disponível";
    }

    /**
     * Gera uma tag disponível para um determinado username.
     *
     * @param username Nome de utilizador.
     * @return Uma tag disponível ou mensagem de erro.
     */
    @GetMapping("/register/generate-tag")
    @ResponseBody
    public String generateTag(@RequestParam("username") String username) {
        for (int i = 0; i <= 9999; i++) {
            String tag = String.format("%04d", i);
            if (!userRepository.existsByUsernameAndTag(username, tag)) {
                return tag;
            }
        }
        return "Nenhuma tag disponível";
    }
}
