package com.algotrade.bot.controller;

import com.algotrade.bot.services.AngelOneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AngelOneService angelOneService;

    public AuthController(AngelOneService angelOneService) {
        this.angelOneService = angelOneService;
    }

    // Show login page. If already logged in, redirect to dashboard.
    @GetMapping({"/", "/login"})
    public String loginRoot(HttpSession session) {
        // If session already has a clientCode (logged in), go to dashboard
        if (session.getAttribute("clientCode") != null && session.getAttribute("jwtToken") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    // Handle login form POST (userId is clientCode)
    @PostMapping("/login")
    public String login(@RequestParam("userId") String userId,
                        @RequestParam("totp") String totp,
                        HttpSession session,
                        Model model) {
        try {
            // login stores tokens & clientCode in session
            angelOneService.login(userId, totp, session);

            // fetch profile and store in session
            Map<String, Object> profile = angelOneService.getProfile(session);
            session.setAttribute("profile", profile);

            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Login failed: " + e.getMessage());
            return "login";
        }
    }

    // logout and redirect to login (/auth/)
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        angelOneService.logout(session);
        return "redirect:/auth/";   // <-- redirect to /auth/ (login page)
    }
}
