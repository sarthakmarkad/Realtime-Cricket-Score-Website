package com.cricket.scores.web;

import com.cricket.scores.dto.Dtos;
import com.cricket.scores.service.CricketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final CricketService cricketService;

    public MatchController(CricketService cricketService) {
        this.cricketService = cricketService;
    }

    @GetMapping
    public List<Dtos.MatchSummary> getMatches() {
        return cricketService.getMatches();
    }

    @GetMapping("/{id}")
    public Dtos.MatchDetail getMatch(@PathVariable Long id) {
        return cricketService.getMatchDetail(id);
    }

    @GetMapping("/{id}/scorecard")
    public List<Dtos.InningsDetail> getScorecard(@PathVariable Long id) {
        return cricketService.getScorecard(id);
    }

    @GetMapping("/{id}/commentary")
    public List<Dtos.CommentaryBall> getCommentary(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "30") int limit) {
        return cricketService.getCommentary(id, limit);
    }

    @GetMapping("/{id}/summary")
    public Dtos.MatchSummaryInfo getSummary(@PathVariable Long id) {
        return cricketService.getMatchSummary(id);
    }

    @PostMapping("/{id}/start")
    public Dtos.MatchSummary startMatch(@PathVariable Long id) {
        return cricketService.startMatch(id);
    }
}
