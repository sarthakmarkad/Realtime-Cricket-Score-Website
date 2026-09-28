package com.cricket.scores.web;

import com.cricket.scores.dto.Dtos;
import com.cricket.scores.service.CricketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final CricketService cricketService;

    public PlayerController(CricketService cricketService) {
        this.cricketService = cricketService;
    }

    @GetMapping("/leaders")
    public Dtos.Leaderboard getLeaders() {
        return cricketService.getLeaderboard();
    }

    @GetMapping("/{id}")
    public Dtos.PlayerProfile getPlayer(@PathVariable Long id) {
        return cricketService.getPlayer(id);
    }
}
