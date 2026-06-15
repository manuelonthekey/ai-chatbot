let stompClient = null;
let typingRow = null;

const viewport      = document.getElementById('messages-viewport');
const messagesInner = document.getElementById('messages-inner');
const input         = document.getElementById('message-input');
const sendBtn       = document.getElementById('send-button');
const statusDot     = document.getElementById('status-dot');
const statusLabel   = document.getElementById('status-label');
const sidebarToggle = document.getElementById('sidebar-toggle');
const sidebar       = document.querySelector('.sidebar');
const newChatBtn    = document.getElementById('new-chat-btn');

/* ── WebSocket connection ── */
function connect() {
    const socket = new SockJS('/chat-websocket');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        setStatus(true);
        sendBtn.disabled = false;

        stompClient.subscribe('/topic/replies', function (frame) {
            removeTypingIndicator();
            const data = JSON.parse(frame.body);
            appendMessage('bot', data.content);
            setInputLocked(false);
        });
    }, function () {
        setStatus(false);
        sendBtn.disabled = true;
        setTimeout(connect, 5000);
    });
}

/* ── Status indicator ── */
function setStatus(online) {
    if (online) {
        statusDot.classList.add('online');
        statusLabel.textContent = 'Online';
    } else {
        statusDot.classList.remove('online');
        statusLabel.textContent = 'Reconnecting…';
    }
}

/* ── Send message ── */
function sendMessage() {
    const text = input.value.trim();
    if (!text || !stompClient) return;

    appendMessage('user', text);
    setInputLocked(true);
    showTypingIndicator();

    stompClient.send('/app/message', {}, JSON.stringify({ content: text, sender: 'User' }));
    input.value = '';
    autoResize();
}

/* ── Render a message row ── */
function appendMessage(role, text) {
    const isBot = role === 'bot';
    const now   = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const row    = document.createElement('div');
    row.classList.add('message-row', role);

    const sender = document.createElement('div');
    sender.classList.add('message-sender');
    sender.textContent = isBot ? 'JavaGPT' : 'You';

    const bubble = document.createElement('div');
    bubble.classList.add('message-bubble');
    bubble.textContent = text;

    const time = document.createElement('div');
    time.classList.add('message-time');
    time.textContent = now;

    row.appendChild(sender);
    row.appendChild(bubble);
    row.appendChild(time);

    messagesInner.appendChild(row);
    scrollToBottom();
}

/* ── Typing indicator ── */
function showTypingIndicator() {
    typingRow = document.createElement('div');
    typingRow.classList.add('typing-row');

    const senderLabel = document.createElement('div');
    senderLabel.classList.add('message-sender');
    senderLabel.style.color = 'var(--teal-500)';
    senderLabel.textContent = 'JavaGPT';

    const bubble = document.createElement('div');
    bubble.classList.add('typing-bubble');
    for (let i = 0; i < 3; i++) {
        const dot = document.createElement('div');
        dot.classList.add('typing-dot');
        bubble.appendChild(dot);
    }

    typingRow.appendChild(senderLabel);
    typingRow.appendChild(bubble);
    messagesInner.appendChild(typingRow);
    scrollToBottom();
}

function removeTypingIndicator() {
    if (typingRow) { typingRow.remove(); typingRow = null; }
}

/* ── Helpers ── */
function setInputLocked(locked) {
    input.disabled  = locked;
    sendBtn.disabled = locked;
    if (!locked) input.focus();
}

function scrollToBottom() {
    viewport.scrollTo({ top: viewport.scrollHeight, behavior: 'smooth' });
}

function autoResize() {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 160) + 'px';
}

/* ── New chat ── */
function clearChat() {
    // Remove all message rows but keep the welcome card
    const rows = messagesInner.querySelectorAll('.message-row, .typing-row');
    rows.forEach(r => r.remove());
}

/* ── Events ── */
sendBtn.addEventListener('click', sendMessage);

input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

input.addEventListener('input', autoResize);

if (sidebarToggle && sidebar) {
    sidebarToggle.addEventListener('click', function () {
        sidebar.classList.toggle('open');
    });
}

if (newChatBtn) {
    newChatBtn.addEventListener('click', function () {
        clearChat();
        if (window.innerWidth <= 720 && sidebar) sidebar.classList.remove('open');
        input.focus();
    });
}

// Close sidebar when clicking outside on mobile
document.addEventListener('click', function (e) {
    if (sidebar && window.innerWidth <= 720
        && sidebar.classList.contains('open')
        && !sidebar.contains(e.target)
        && e.target !== sidebarToggle) {
        sidebar.classList.remove('open');
    }
});

connect();
