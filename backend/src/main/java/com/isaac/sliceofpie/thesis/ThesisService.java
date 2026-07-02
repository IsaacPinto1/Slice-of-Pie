package com.isaac.sliceofpie.thesis;

import org.springframework.stereotype.Service;
import com.isaac.sliceofpie.users.UserRepository;


@Service
public class ThesisService {

    private final ThesisRepository thesisRepository;
    private final UserRepository userRepository;

    public ThesisService(ThesisRepository thesisRepository,
                         UserRepository userRepository) {
        this.thesisRepository = thesisRepository;
        this.userRepository = userRepository;
    }

    public Thesis upsert(String username, String ticker, String content) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        return thesisRepository.findByUserIdAndTicker(userId, ticker)
                .map(existing -> {
                    existing.setContent(content);
                    return thesisRepository.save(existing);
                })
                .orElseGet(() -> {
                    Thesis t = new Thesis();
                    t.setUserId(userId);
                    t.setTicker(ticker);
                    t.setContent(content);
                    return thesisRepository.save(t);
                });
    }

    public Thesis get(String username, String ticker) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow()
                .getId();

        return thesisRepository.findByUserIdAndTicker(userId, ticker)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }
}