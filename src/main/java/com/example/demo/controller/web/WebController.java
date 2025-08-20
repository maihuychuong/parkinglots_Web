package com.example.demo.controller.web;

import com.example.demo.config.CustomUserDetails;
import com.example.demo.entity.User;
import com.example.demo.model.enums.UserStatus;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;
import java.util.regex.Pattern;

@Controller
public class WebController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final Pattern passwordPattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,}$");

    public WebController(AuthenticationManager authenticationManager, JwtService jwtService,
                         CustomUserDetailsService userDetailsService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginProcess(@RequestParam String username,
                               @RequestParam String password,
                               Model model,
                               HttpServletResponse response,
                               HttpServletRequest request) {
        try {
            // Clear any existing authentication context
            SecurityContextHolder.clearContext();

            // Clear existing JWT cookies
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwtToken".equals(cookie.getName())) {
                        Cookie clearCookie = new Cookie("jwtToken", "");
                        clearCookie.setPath("/");
                        clearCookie.setHttpOnly(true);
                        clearCookie.setMaxAge(0);
                        response.addCookie(clearCookie);
                        break;
                    }
                }
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UserStatus status = userDetails.getUser().getStatus();

            if (status == UserStatus.INACTIVE) {
                model.addAttribute("error", "Tài khoản chưa kích hoạt hoặc chưa đến giờ làm việc.");
                return "login";
            } else if (status == UserStatus.BANNED) {
                model.addAttribute("error", "Tài khoản đã bị khóa.");
                return "login";
            } else if (status != UserStatus.ACTIVE) {
                model.addAttribute("error", "Trạng thái tài khoản không hợp lệ.");
                return "login";
            }

            // Generate new JWT token
            String jwt = jwtService.generateToken(userDetails);

            // Set new JWT cookie
            Cookie jwtCookie = new Cookie("jwtToken", jwt);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 hours
            jwtCookie.setSecure(false); // Set to true if using HTTPS
            response.addCookie(jwtCookie);

            // Redirect based on role
            String role = userDetails.getUser().getRole().name();
            if ("ADMIN".equals(role)) {
                return "redirect:/admin";
            } else if ("STAFF".equals(role)) {
                return "redirect:/staff";
            } else {
                model.addAttribute("error", "Role không hợp lệ");
                return "login";
            }

        } catch (AuthenticationException e) {
            model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra trong quá trình đăng nhập");
            return "login";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, HttpServletRequest request, Model model) {
        User user = userService.findByEmail(email);

        if (user == null) {
            model.addAttribute("error", "Email không tồn tại trong hệ thống.");
            return "forgot-password";
        }

        String token = UUID.randomUUID().toString();
        userService.createPasswordResetToken(user, token);

        String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        boolean isSent = userService.sendPasswordResetEmail(user, appUrl, token);

        if (isSent) {
            model.addAttribute("success", "Email đặt lại mật khẩu đã được gửi, vui lòng kiểm tra hộp thư.");
            return "forgot-password";
        } else {
            model.addAttribute("error", "Có lỗi xảy ra khi gửi email, vui lòng thử lại.");
            return "forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam String token, Model model) {
        if (userService.isValidToken(token)) {
            model.addAttribute("token", token);
            return "reset-password";
        }
        return "redirect:/forgot-password?error";
    }

    @PostMapping("/reset-password")
    public String handleReset(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmNewPassword,
            Model model) {
        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            return "reset-password";
        }

        if (!passwordPattern.matcher(newPassword).matches()) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Mật khẩu phải chứa ít nhất một chữ cái và một số.");
            return "reset-password";
        }

        if (!newPassword.equals(confirmNewPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Mật khẩu mới và xác nhận không khớp!");
            return "reset-password";
        }

        if (userService.resetPassword(token, newPassword)) {
            model.addAttribute("success", "Đổi mật khẩu thành công! Bạn sẽ được chuyển đến trang đăng nhập sau vài giây...");
            return "reset-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn!");
        return "reset-password";
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwtToken", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}