window.eyeCodeLearningUpdate = function(payload) {
    if (!payload || typeof payload.html !== "string") return;
    const target = document.getElementById("learning-content");
    if (!target) return;
    const parsed = new DOMParser().parseFromString(payload.html, "text/html");
    const source = parsed.querySelector(".learning-content") || parsed.body;
    target.innerHTML = source ? source.innerHTML : "";
    window.scrollTo(0, 0);
    if (window.hljs && typeof window.hljs.highlightAll === "function") {
        window.hljs.highlightAll();
    }
    if (window.cefQuery) {
        window.cefQuery({ request: JSON.stringify({ kind: "contentApplied" }) });
    }
};

document.addEventListener("DOMContentLoaded", () => {
    if (window.hljs && typeof window.hljs.highlightAll === "function") {
        window.hljs.highlightAll();
    }
    if (window.cefQuery) {
        window.cefQuery({ request: JSON.stringify({ kind: "learningReady" }) });
    }
});
