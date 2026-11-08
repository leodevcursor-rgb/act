/* ACT Squad · cliente da API Spring Boot (/api/v1) */
(function (global) {
  const LS_TOKEN = "act-api-token";
  const LS_USER = "act-api-user";
  const LS_BASE = "act-api-base";

  /* Servido pelo nginx (http/https): mesma origem, o nginx faz proxy de /api/.
     Aberto direto do disco (file://): tenta a API local na 8080. */
  function fallbackBase() {
    const proto = location.protocol;
    if (proto === "http:" || proto === "https:") return location.origin;
    return "http://localhost:8080";
  }

  function defaultBase() {
    try {
      const q = new URLSearchParams(location.search).get("api");
      if (q) {
        const b = q.replace(/\/$/, "");
        try {
          localStorage.setItem(LS_BASE, b);
        } catch (e) {}
        return b;
      }
    } catch (e) {}
    // Em http(s) use sempre a mesma origem (nginx → /api/). Evita base antiga no localStorage.
    const proto = location.protocol;
    if (proto === "http:" || proto === "https:") return location.origin;
    try {
      const saved = localStorage.getItem(LS_BASE);
      if (saved) return saved.replace(/\/$/, "");
    } catch (e) {}
    return fallbackBase();
  }

  const Api = {
    base: defaultBase(),
    token: null,
    user: null,

    load() {
      try {
        this.token = localStorage.getItem(LS_TOKEN);
        this.user = JSON.parse(localStorage.getItem(LS_USER) || "null");
      } catch (e) {
        this.token = null;
        this.user = null;
      }
    },

    saveSession(token, user) {
      this.token = token;
      this.user = user;
      try {
        localStorage.setItem(LS_TOKEN, token);
        localStorage.setItem(LS_USER, JSON.stringify(user));
      } catch (e) {}
    },

    clearSession() {
      this.token = null;
      this.user = null;
      try {
        localStorage.removeItem(LS_TOKEN);
        localStorage.removeItem(LS_USER);
      } catch (e) {}
    },

    loggedIn() {
      return !!this.token;
    },

    setBase(url) {
      this.base = (url || "").replace(/\/$/, "") || fallbackBase();
      try {
        localStorage.setItem(LS_BASE, this.base);
      } catch (e) {}
    },

    async request(method, path, body, opts) {
      const headers = { Accept: "application/json" };
      if (body !== undefined) headers["Content-Type"] = "application/json";
      if (this.token) headers.Authorization = "Bearer " + this.token;
      let res;
      try {
        res = await fetch(this.base + path, {
          method,
          headers,
          body: body !== undefined ? JSON.stringify(body) : undefined,
          keepalive: !!(opts && opts.keepalive),
        });
      } catch (e) {
        const err = new Error("Não consegui falar com o servidor. Confira sua conexão e tente de novo.");
        err.code = "network";
        throw err;
      }
      if (res.status === 204) return null;
      const text = await res.text();
      let data = null;
      try {
        data = text ? JSON.parse(text) : null;
      } catch (e) {
        data = { error: text };
      }
      if (!res.ok) {
        const msg = (data && (data.error || data.message)) || "Erro " + res.status;
        /* 401 = sessão inválida. 403 genérico "Forbidden" também costuma ser JWT ausente/inválido
           (Spring Security anônimo) — limpa a sessão para não prender a tela de login. */
        const authGone =
          res.status === 401 ||
          (res.status === 403 &&
            /^(Forbidden|Access Denied)$/i.test(String(msg).trim()));
        if (authGone && this.token) {
          this.clearSession();
          try {
            global.dispatchEvent(new CustomEvent("actsquad:unauthorized"));
          } catch (e) {}
        }
        const err = new Error(msg);
        err.status = res.status;
        err.data = data;
        throw err;
      }
      return data;
    },

    get(path) {
      return this.request("GET", path);
    },
    post(path, body) {
      return this.request("POST", path, body);
    },
    put(path, body, opts) {
      return this.request("PUT", path, body, opts);
    },
    patch(path, body) {
      return this.request("PATCH", path, body);
    },
    del(path) {
      return this.request("DELETE", path);
    },

    register(email, password, name) {
      return this.post("/api/v1/auth/register", { email, password, name });
    },
    login(email, password) {
      return this.post("/api/v1/auth/login", { email, password });
    },
    me() {
      return this.get("/api/v1/me");
    },
    termsStatus() {
      return this.get("/api/v1/terms");
    },
    acceptTerms(termsVersion) {
      return this.post("/api/v1/terms/accept", termsVersion ? { termsVersion } : {});
    },
    listPatients() {
      return this.get("/api/v1/patients");
    },
    createPatient(displayName, cpf, note) {
      return this.post("/api/v1/patients", { displayName, cpf, note: note || null });
    },
    getPatient(id) {
      return this.get("/api/v1/patients/" + id);
    },
    /* patch: { displayName?, note? } — note "" limpa a nota */
    updatePatient(id, patch) {
      return this.patch("/api/v1/patients/" + id, patch);
    },
    deletePatient(id) {
      return this.del("/api/v1/patients/" + id);
    },
    getState(id) {
      return this.get("/api/v1/patients/" + id + "/state");
    },
    /* opts.keepalive: permite concluir o PUT ao fechar a aba */
    putState(id, state, opts) {
      return this.put("/api/v1/patients/" + id + "/state", state, opts);
    },
  };

  Api.load();
  global.ActSquadApi = Api;
})(window);
