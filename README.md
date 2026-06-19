# JavaGPT — AI Chatbot

A Spring Boot AI chatbot powered by Groq (Llama 3.1) with a real-time WebSocket UI.

---

## Prerequisites

- **Java 17+**
- **Maven 3.9+** (or use your IDE's built-in Maven)
- A **Groq API key** — get one free at [console.groq.com/keys](https://console.groq.com/keys)

---

## Running Locally

1. **Set your API key** as an environment variable:

   ```bash
   # Linux / macOS
   export GROQ_API_KEY=gsk_your_key_here

   # Windows (PowerShell)
   $env:GROQ_API_KEY="gsk_your_key_here"
   ```

2. **Build & run:**

   ```bash
   mvn clean package -DskipTests
   java -jar target/ai-chatbot-0.0.1-SNAPSHOT.jar
   ```

3. Open **http://localhost:8081** in your browser.

---

## Deployment

### Option 1 — Docker

```bash
# Build the image
docker build -t javagpt .

# Run the container
docker run -d -p 8080:8080 -e GROQ_API_KEY=gsk_your_key_here javagpt
```

### Option 2 — Railway / Render / Fly.io

1. Push this repo to GitHub.
2. Connect the repo in your platform's dashboard.
3. Set these **environment variables** on the platform:
   | Variable | Value |
   |---|---|
   | `GROQ_API_KEY` | Your Groq API key |
   | `PORT` | `8080` (usually auto-set) |
   | `SPRING_PROFILES_ACTIVE` | `prod` |
4. Deploy. The Dockerfile will be auto-detected.

### Option 3 — Manual JAR Deployment

```bash
mvn clean package -DskipTests
GROQ_API_KEY=gsk_your_key SPRING_PROFILES_ACTIVE=prod java -jar target/ai-chatbot-0.0.1-SNAPSHOT.jar
```

---

## Project Structure

```
ai-chatbot/
├── src/main/java/com/chatbot/
│   ├── ChatbotApplication.java      # Entry point
│   ├── config/WebSocketConfig.java   # WebSocket/STOMP config
│   ├── controller/ChatController.java
│   ├── model/                        # ChatMessage, ChatResponse
│   └── service/
│       ├── GroqService.java          # Groq API integration
│       └── NlpService.java           # Local NLP fallback
├── src/main/resources/
│   ├── application.properties        # Base config
│   ├── application-prod.properties   # Production overrides
│   └── static/                       # Frontend (HTML/CSS/JS)
├── Dockerfile
├── pom.xml
└── .gitignore
```

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GROQ_API_KEY` | ✅ Yes | — | Your Groq API key |
| `PORT` | No | `8081` | Server port |
| `SPRING_PROFILES_ACTIVE` | No | `default` | Set to `prod` for production |
