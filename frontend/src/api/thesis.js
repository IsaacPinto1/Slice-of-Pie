import api from "./axios";

export function getThesis(instrumentId) {
    return api.get(`/thesis/${instrumentId}`);
}

export function saveThesis(instrumentId, content) {
    return api.post("/thesis", {
        instrumentId,
        content
    });
}
