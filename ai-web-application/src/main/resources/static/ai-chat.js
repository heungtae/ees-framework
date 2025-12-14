// Copied from sample application for standalone AI web.
(function () {
    const transcript = document.getElementById("transcript");
    const statusEl = document.getElementById("status");
    const sseStatus = document.getElementById("sseStatus");

    const sessionIdEl = document.getElementById("sessionId");
    const userIdEl = document.getElementById("userId");
    const toolsAllowedEl = document.getElementById("toolsAllowed");
    const approveEl = document.getElementById("approve");
    const streamingEl = document.getElementById("streaming");
    const messageEl = document.getElementById("message");

    const sendBtn = document.getElementById("send");
    const sendStreamBtn = document.getElementById("sendStream");
    const clearBtn = document.getElementById("clear");

    function uuid() {
        if (crypto && crypto.randomUUID) return crypto.randomUUID();
        return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
            const r = (Math.random() * 16) | 0;
            const v = c === "x" ? r : (r & 0x3) | 0x8;
            return v.toString(16);
        });
    }

    function nowText() {
        return new Date().toLocaleString();
    }

    function appendLine(text, cls) {
        const div = document.createElement("div");
        if (cls) div.className = cls;
        div.textContent = text;
        transcript.appendChild(div);
        transcript.scrollTop = transcript.scrollHeight;
    }

    function showError(text) {
        appendLine(`[error] ${text}`, "error");
    }

    function setStatus(text) {
        statusEl.textContent = text ? `(${text})` : "";
    }

    function parseToolsAllowed() {
        const raw = toolsAllowedEl.value || "";
        return raw
            .split(",")
            .map((s) => s.trim())
            .filter((s) => s.length > 0);
    }

    function buildRequest() {
        const message = (messageEl.value || "").trim();
        if (!message) throw new Error("message is empty");
        return {
            sessionId: (sessionIdEl.value || "").trim(),
            userId: (userIdEl.value || "").trim(),
            messages: [{ role: "user", content: message }],
            toolsAllowed: parseToolsAllowed(),
            streaming: streamingEl.checked,
        };
    }

    function headers() {
        const h = { "Content-Type": "application/json" };
        if (approveEl.checked) h["X-AI-Approve"] = "true";
        return h;
    }

    async function chat() {
        setStatus("sending...");
        try {
            const req = buildRequest();
            const res = await fetch("/api/ai/chat", {
                method: "POST",
                headers: headers(),
                body: JSON.stringify(req),
            });
            const text = await res.text();
            if (!res.ok) {
                showError(`${res.status} ${text}`);
                return;
            }
            appendLine(`[${nowText()}] ${text}`);
        } catch (e) {
            showError(e.message || String(e));
        } finally {
            setStatus("");
        }
    }

    async function chatStream() {
        setStatus("streaming...");
        sseStatus.textContent = "connecting";
        try {
            const req = buildRequest();
            const res = await fetch("/api/ai/chat/stream", {
                method: "POST",
                headers: headers(),
                body: JSON.stringify(req),
            });
            if (!res.ok) {
                const text = await res.text();
                showError(`${res.status} ${text}`);
                sseStatus.textContent = "error";
                return;
            }

            sseStatus.textContent = "open";
            const reader = res.body.getReader();
            const decoder = new TextDecoder("utf-8");
            let buffer = "";
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                buffer += decoder.decode(value, { stream: true });
                const parts = buffer.split("\n\n");
                buffer = parts.pop() || "";
                for (const part of parts) {
                    if (part.startsWith("data:")) {
                        const data = part.replace(/^data:\\s*/gm, "");
                        appendLine(data);
                    }
                }
            }
            sseStatus.textContent = "closed";
        } catch (e) {
            showError(e.message || String(e));
            sseStatus.textContent = "error";
        } finally {
            setStatus("");
        }
    }

    clearBtn.addEventListener("click", () => {
        transcript.innerHTML = "";
        setStatus("");
        sseStatus.textContent = "idle";
    });

    sendBtn.addEventListener("click", chat);
    sendStreamBtn.addEventListener("click", chatStream);

    messageEl.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
            e.preventDefault();
            chat();
        }
    });

    if (!sessionIdEl.value) {
        sessionIdEl.value = uuid();
    }
})();

