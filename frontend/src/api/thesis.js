import api from "./axios";

export function getThesis(ticker) {
    return api.get(`/thesis/${ticker}`);
}

export function saveThesis(ticker, content) {

    return api.post("/thesis", {
        ticker,
        content
    });

}