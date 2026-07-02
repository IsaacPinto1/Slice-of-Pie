import api from "./axios";

export function login(username, password) {
    return api.post("/auth/login", {
        username,
        password
    });
}

export function register(username, password) {
    return api.post("/auth/register", {
        username,
        password
    });
}