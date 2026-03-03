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
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String bio,
            @RequestParam(required = false) String profilePicturePath,
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
    public String updatePrivacy(@RequestParam String username,
            @RequestParam(required = false) String newPassword,
            HttpSession session,
            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login";

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(username);
            if (newPassword != null && !newPassword.isEmpty()) {
                user.setPassword(newPassword);
            }
            userRepository.save(user);
        }
        return "redirect:/profile";
    }
}
