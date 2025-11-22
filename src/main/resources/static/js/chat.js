document.addEventListener("DOMContentLoaded", function() {
    const sendButton = document.getElementById("send-button");
    const chatInput = document.getElementById("chat-input");
    const messagesContainer = document.getElementById("messages");

    sendButton.addEventListener("click", function() {
        const prompt = chatInput.value;
        if (!prompt) return;
        chatInput.value = "";

        // Add the user's message to the chat
        const userDiv = document.createElement("div");
        userDiv.className = "message user";
        userDiv.innerHTML = `<img src="/images/user.png" alt="User"><div class="bubble">${prompt}</div>`;
        messagesContainer.appendChild(userDiv);

        const pathParts = window.location.pathname.split("/");
        const chatId = pathParts[pathParts.length - 1];
        const url = `/chat-stream/${chatId}?userPrompt=${encodeURIComponent(prompt)}`;

        const eventSource = new EventSource(url);
        let fullText = "";

        // Create a block for the AI response
        const aiDiv = document.createElement("div");
        aiDiv.className = "message mentor";
        // Add the assistant's image
        aiDiv.innerHTML = `<img src="/images/mentor.png" alt="Mentor">`;
        // Create an element for the content where the response will be inserted
        const aiBubble = document.createElement("div");
        aiBubble.className = "bubble";
        aiDiv.appendChild(aiBubble);
        messagesContainer.appendChild(aiDiv);

        eventSource.onmessage = function(event) {
            const data = JSON.parse(event.data);
            let token = data.text;
            console.log(token);
            fullText += token;
            // Convert Markdown to HTML (assuming marked.js is connected)
            aiBubble.innerHTML = marked.parse(fullText);
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        };

        eventSource.onerror = function(e) {
            console.error("SSE error:", e);
            eventSource.close();
        };
    });
});
