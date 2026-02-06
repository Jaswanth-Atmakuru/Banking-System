package com.example.bankapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bankapp.model.User;
import com.example.bankapp.repository.UserRepository;

@Controller
public class DashboardController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.example.bankapp.repository.TransactionRepository transactionRepository;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userRepository.findByUsername(username);
        model.addAttribute("account", user);
        model.addAttribute("transactions", transactionRepository.findByUsernameOrderByTimestampDesc(username));
        
        return "dashboard";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam("amount") double amount) {
        if (amount <= 0) {
            return "redirect:/dashboard";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return "redirect:/login";
        }
        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);
        transactionRepository.save(new com.example.bankapp.model.Transaction(username, "Deposit", amount));
        return "redirect:/dashboard";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam("amount") double amount, RedirectAttributes ra) {
        if (amount <= 0) {
            ra.addFlashAttribute("error", "Invalid amount");
            return "redirect:/dashboard";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getBalance() < amount) {
            ra.addFlashAttribute("error", "Insufficient balance");
            return "redirect:/dashboard";
        }
        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);
        transactionRepository.save(new com.example.bankapp.model.Transaction(username, "Withdraw", amount));
        return "redirect:/dashboard";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam("toUsername") String toUsername, @RequestParam("amount") double amount, RedirectAttributes ra) {
        if (amount <= 0) {
            ra.addFlashAttribute("error", "Invalid amount");
            return "redirect:/dashboard";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        if (username.equals(toUsername)) {
            ra.addFlashAttribute("error", "Cannot transfer to yourself");
            return "redirect:/dashboard";
        }
        User fromUser = userRepository.findByUsername(username);
        User toUser = userRepository.findByUsername(toUsername);
        if (fromUser == null) {
            return "redirect:/login";
        }
        if (toUser == null) {
            ra.addFlashAttribute("error", "Recipient not found");
            return "redirect:/dashboard";
        }
        if (fromUser.getBalance() < amount) {
            ra.addFlashAttribute("error", "Insufficient balance");
            return "redirect:/dashboard";
        }
        fromUser.setBalance(fromUser.getBalance() - amount);
        toUser.setBalance(toUser.getBalance() + amount);
        userRepository.save(fromUser);
        userRepository.save(toUser);
        transactionRepository.save(new com.example.bankapp.model.Transaction(username, "Transfer Out to " + toUsername, amount));
        transactionRepository.save(new com.example.bankapp.model.Transaction(toUsername, "Transfer In from " + username, amount));
        return "redirect:/dashboard";
    }

    @GetMapping("/transactions")
    public String transactions(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("transactions", transactionRepository.findByUsernameOrderByTimestampDesc(username));
        return "transactions";
    }
}
