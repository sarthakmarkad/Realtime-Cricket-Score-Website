package com.cricket.scores.web;

import com.cricket.scores.dto.Dtos;
import com.cricket.scores.service.CricketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final CricketService cricketService;

    public TeamController(CricketService cricketService) {
        this.cricketService = cricketService;
    }

    @GetMapping
    public List<Dtos.TeamRoster> getTeams() {
        return cricketService.getTeams();
    }
}
