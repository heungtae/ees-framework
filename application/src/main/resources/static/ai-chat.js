(function () {
    const state = {
        transcript: [],
        liveText: "",
        streamingAbort: null,
        isLoading: false
    };

    const el = (id) => document.getElementById(id);

    document.addEventListener("DOMContentLoaded", () => {
        initializeDefaults();
        wireEvents();
    });

    function initializeDefaults() {
        el("sessionId").value = crypto.randomUUID();
        el("userId").value = "web-user";
        updateSendButtonLabel();
        setLoading(false, "대기");
        setSseStatus("대기", "status-idle");
        setChunkStatus("대기");
    }

    function wireEvents() {
        el("regenSession").addEventListener("click", () => {
            el("sessionId").value = crypto.randomUUID();
        });

        el("sendBtn").addEventListener("click", handleSend);
        el("stopBtn").addEventListener("click", handleStop);
        el("clearBtn").addEventListener("click", clearTranscript);
        el("streamingToggle").addEventListener("change", updateSendButtonLabel);

        el("prompt").addEventListener("keydown", (event) => {
            if (event.key === "Enter" && event.ctrlKey) {
                event.preventDefault();
                handleSend();
            }
        });
    }

    function updateSendButtonLabel() {
        const streaming = el("streamingToggle").checked;
        el("sendBtn").textContent = streaming ? "스트림 전송" : "동기 전송";
    }

    function handleSend() {
        clearError();
        if (state.isLoading) {
            return;
        }
        const payload = buildPayload();
        if (!payload) {
            return;
        }
        const userMessage = payload.messages[0].content;
        pushTranscript("user", userMessage);
        el("prompt").value = "";
        updateLastRequestTime();
        if (payload.streaming) {
            startStreaming(payload);
        } else {
            sendSync(payload);
        }
    }

    function handleStop() {
        if (state.streamingAbort) {
            state.streamingAbort.abort();
        }
    }

    function buildPayload() {
        const sessionId = el("sessionId").value.trim() || crypto.randomUUID();
        const userId = el("userId").value.trim() || "web-user";
        el("sessionId").value = sessionId;
        const content = el("prompt").value.trim();
        if (!content) {
            showError("메시지를 입력하세요.");
            return null;
        }
        const toolsAllowed = el("toolsAllowed").value
            .split(",")
            .map((t) => t.trim())
            .filter(Boolean);
        return {
            sessionId,
            userId,
            messages: [{ role: "user", content }],
            toolsAllowed,
            streaming: el("streamingToggle").checked
        };
    }

    function buildHeaders(forStream) {
        const headers = {
            "Content-Type": "application/json"
        };
        if (forStream) {
            headers["Accept"] = "text/event-stream";
        }
        if (el("approveToggle").checked) {
            headers["X-AI-Approve"] = "true";
        }
        return headers;
    }

    async function sendSync(payload) {
        setLoading(true, "요청 중...");
        setSseStatus("동기 호출", "status-busy");
        try {
            const res = await fetch("/api/ai/chat", {
                method: "POST",
                headers: buildHeaders(false),
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                await handleHttpError(res);
                setSseStatus("오류", "status-error");
                return;
            }
            const data = await res.json();
            pushTranscript("assistant", data.content);
            setSseStatus("완료", "status-ok");
            setChunkStatus("대기");
        } catch (err) {
            showError(`요청 실패: ${err.message}`);
            setSseStatus("오류", "status-error");
        } finally {
            setLoading(false, "대기");
        }
    }

    async function startStreaming(payload) {
        setLoading(true, "스트리밍 연결 중...");
        setSseStatus("연결 시도", "status-busy");
        setChunkStatus("연결 중");
        state.liveText = "";
        el("streamOutput").textContent = "";

        const controller = new AbortController();
        state.streamingAbort = controller;
        el("stopBtn").disabled = false;

        try {
            const res = await fetch("/api/ai/chat/stream", {
                method: "POST",
                headers: buildHeaders(true),
                body: JSON.stringify(payload),
                signal: controller.signal
            });
            if (!res.ok) {
                await handleHttpError(res);
                setSseStatus("오류", "status-error");
                setChunkStatus("실패");
                return;
            }
            if (!res.body) {
                showError("SSE 스트림을 열 수 없습니다.");
                setSseStatus("오류", "status-error");
                setChunkStatus("실패");
                return;
            }
            setSseStatus("수신 중", "status-busy");
            await readSseStream(res.body);
            if (state.liveText) {
                pushTranscript("assistant", state.liveText);
            }
            setSseStatus("완료", "status-ok");
            setChunkStatus("완료");
        } catch (err) {
            if (err.name === "AbortError") {
                showError("스트리밍이 중단되었습니다.");
                setSseStatus("중단", "status-error");
                setChunkStatus("중단");
            } else {
                showError(`스트리밍 오류: ${err.message}`);
                setSseStatus("오류", "status-error");
                setChunkStatus("실패");
            }
        } finally {
            setLoading(false, "대기");
            el("stopBtn").disabled = true;
            state.streamingAbort = null;
        }
    }

    async function readSseStream(body) {
        const reader = body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                break;
            }
            buffer += decoder.decode(value, { stream: true });
            let boundary = buffer.indexOf("\n\n");
            while (boundary !== -1) {
                const rawEvent = buffer.slice(0, boundary);
                buffer = buffer.slice(boundary + 2);
                processSseEvent(rawEvent);
                boundary = buffer.indexOf("\n\n");
            }
        }
        if (buffer.trim()) {
            processSseEvent(buffer);
        }
    }

    function processSseEvent(rawEvent) {
        const lines = rawEvent.split("\n");
        let event = "message";
        let data = "";
        lines.forEach((line) => {
            if (line.startsWith("event:")) {
                event = line.slice(6).trim();
            }
            if (line.startsWith("data:")) {
                data += line.slice(5).trim();
            }
        });
        if (!data) {
            return;
        }
        try {
            const payload = JSON.parse(data);
            if (event === "chunk" || payload.streaming) {
                state.liveText += payload.content;
                el("streamOutput").textContent = state.liveText;
                setChunkStatus("수신 중");
            } else {
                state.liveText = payload.content;
                el("streamOutput").textContent = payload.content;
                setChunkStatus("완료");
            }
        } catch (err) {
            showError(`SSE 파싱 오류: ${err.message}`);
            setChunkStatus("실패");
        }
    }

    async function handleHttpError(res) {
        let message = `${res.status} ${res.statusText}`;
        try {
            const data = await res.json();
            if (data && data.message) {
                message = data.message;
            }
        } catch {
            try {
                const text = await res.text();
                if (text) {
                    message = text;
                }
            } catch {
                // ignore
            }
        }
        if (message.includes("Dangerous tools")) {
            message += " (X-AI-Approve 헤더를 체크하세요)";
        }
        showError(message);
    }

    function pushTranscript(role, content) {
        state.transcript.push({
            role,
            content,
            time: new Date()
        });
        renderTranscript();
    }

    function renderTranscript() {
        const container = el("transcript");
        container.innerHTML = "";
        if (!state.transcript.length) {
            const placeholder = document.createElement("div");
            placeholder.className = "placeholder";
            placeholder.textContent = "아직 메시지가 없습니다. 프롬프트를 전송해 응답을 받아보세요.";
            container.appendChild(placeholder);
            return;
        }
        state.transcript.forEach((item) => {
            const bubble = document.createElement("div");
            bubble.className = `bubble bubble--${item.role === "user" ? "user" : "assistant"}`;

            const meta = document.createElement("div");
            meta.className = "bubble__meta";
            meta.textContent = `${item.role.toUpperCase()} · ${item.time.toLocaleTimeString()}`;

            const body = document.createElement("div");
            body.textContent = item.content;

            bubble.appendChild(meta);
            bubble.appendChild(body);
            container.appendChild(bubble);
        });
    }

    function clearTranscript() {
        state.transcript = [];
        state.liveText = "";
        el("streamOutput").textContent = "";
        renderTranscript();
        clearError();
        setChunkStatus("대기");
    }

    function showError(message) {
        const box = el("errorBox");
        box.textContent = message;
        box.hidden = false;
    }

    function clearError() {
        const box = el("errorBox");
        box.textContent = "";
        box.hidden = true;
    }

    function setLoading(loading, label) {
        state.isLoading = loading;
        el("sendBtn").disabled = loading;
        el("regenSession").disabled = loading;
        el("clearBtn").disabled = loading;
        el("loadingText").textContent = label;
    }

    function setSseStatus(text, cssClass) {
        el("sseStatus").textContent = text;
        el("loadingDot").className = `dot ${cssClass}`;
    }

    function setChunkStatus(text) {
        el("chunkStatus").textContent = text;
    }

    function updateLastRequestTime() {
        el("lastRequest").textContent = new Date().toLocaleTimeString();
    }
})();
