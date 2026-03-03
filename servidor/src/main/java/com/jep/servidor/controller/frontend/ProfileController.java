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
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        userRepository.findById(userId).ifPresent(user -> model.addAttribute("user", user));

        // Passar mensagens temporárias da sessão para o model e limpar
        model.addAttribute("error", session.getAttribute("error"));
        model.addAttribute("success", session.getAttribute("success"));
        session.removeAttribute("error");
        session.removeAttribute("success");

        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("bio") String bio,
            @RequestParam(value = "profilePicturePath", required = false) String profilePicturePath,
            HttpSession session,
            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login";

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setBio(bio);
            if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                user.setProfilePicturePath(profilePicturePath);
            }
            userRepository.save(user);
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/privacy")
    public String updatePrivacy(@RequestParam("username") String username,
            HttpSession session,
            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login";

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(username);
            userRepository.save(user);
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login";

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Validações básicas
            if (!user.getPassword().equals(currentPassword)) {
                session.setAttribute("error", "A password atual está incorreta.");
                return "redirect:/profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                session.setAttribute("error", "As novas passwords não coincidem.");
                return "redirect:/profile";
            }
            if (newPassword.isEmpty()) {
                session.setAttribute("error", "A nova password não pode estar vazia.");
                return "redirect:/profile";
            }

            user.setPassword(newPassword);
            userRepository.save(user);
            session.setAttribute("success", "Password alterada com sucesso!");
        }
        return "redirect:/profile";
    }
}
