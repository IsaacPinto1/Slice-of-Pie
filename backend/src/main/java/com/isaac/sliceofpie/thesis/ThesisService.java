package com.isaac.sliceofpie.thesis;

import org.springframework.stereotype.Service;


@Service
public class ThesisService {

    private final ThesisRepository thesisRepository;

    public ThesisService(ThesisRepository thesisRepository) {
        this.thesisRepository = thesisRepository;
    }

    public Thesis upsert(Long userId, String ticker, String content) {

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

    public Thesis get(Long userId, String ticker) {
        return thesisRepository.findByUserIdAndTicker(userId, ticker)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }
}