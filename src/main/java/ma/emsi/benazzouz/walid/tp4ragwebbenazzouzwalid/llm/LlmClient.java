package ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LlmClient {

    private Assistant assistant;
    private ChatMemory memory;

    public LlmClient() {

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("⚠️ Définis GEMINI_API_KEY dans les variables d'environnement !");
        }

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-flash")
                .temperature(0.4)
                .build();

        this.memory = MessageWindowChatMemory.withMaxMessages(10);

        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(memory)
                .build();
    }

    /** ✅ Méthode appelée lors du premier message dans Bb */
    public void applySystemRole(String systemRole) {
        memory.clear();
        memory.add(new SystemMessage(systemRole));
    }

    /** ✅ Utilisée par le Bean pour envoyer un message */
    public String ask(String userMessage) {
        return assistant.chat(userMessage);
    }
}
