package ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.llm;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service RAG avec routage thématique.
 * Selon le sujet de la question, on interroge soit des documents liés à l'IA, soit au sport.
 */
@ApplicationScoped
public class LlmCR{

    private final Assistant agent;

    public LlmCR() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("⚠️ Définis GEMINI_API_KEY dans les variables d'environnement.");
        }

        // Modèle de chat (Gemini)
        ChatModel gemini = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-flash")
                .temperature(0.3)
                .build();

        // Parser de documents PDF
        var parser = new ApacheTikaDocumentParser();
        EmbeddingModel embedder = new AllMiniLmL6V2EmbeddingModel();

        // Chargement des ressources PDF
        Path fileIA = loadResource("support_rag.pdf");
        Path fileSport = loadResource("voiture.pdf");

        // Extraction du texte
        Document docIA = FileSystemDocumentLoader.loadDocument(fileIA, parser);
        Document docSport = FileSystemDocumentLoader.loadDocument(fileSport, parser);

        // Découpage en segments exploitables par l’embedding
        var splitter = DocumentSplitters.recursive(300, 40);
        List<TextSegment> segIA = splitter.split(docIA);
        List<TextSegment> segSport = splitter.split(docSport);

        // Boutiques d’embeddings en RAM
        EmbeddingStore<TextSegment> storeIA = new InMemoryEmbeddingStore<>();
        EmbeddingStore<TextSegment> storeSport = new InMemoryEmbeddingStore<>();

        storeIA.addAll(embedder.embedAll(segIA).content(), segIA);
        storeSport.addAll(embedder.embedAll(segSport).content(), segSport);

        // Deux récupérateurs indépendants
        ContentRetriever retrieverIA = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(storeIA)
                .embeddingModel(embedder)
                .maxResults(4)
                .minScore(0.45)
                .build();

        ContentRetriever retrieverSport = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(storeSport)
                .embeddingModel(embedder)
                .maxResults(4)
                .minScore(0.45)
                .build();

        // Routage en fonction du contenu de la question
        Map<ContentRetriever, String> routingMap = new HashMap<>();
        routingMap.put(retrieverIA, "Informations sur IA, RAG, fine-tuning, modèles LLM.");
        routingMap.put(retrieverSport, "Contenu lié au sport, entraînement, physiologie, performance.");

        var router = new LanguageModelQueryRouter(gemini, routingMap);

        // Augmentation RAG pour améliorer la réponse
        RetrievalAugmentor ragLayer = DefaultRetrievalAugmentor.builder()
                .queryRouter(router)
                .build();

        // Assistant conversationnel final
        this.agent = AiServices.builder(Assistant.class)
                .chatModel(gemini)
                .retrievalAugmentor(ragLayer)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * Pose une question au modèle, avec routage automatique vers les bons documents.
     */
    public String ask(String input) {
        return agent.chat(input);
    }

    /**
     * Utilitaire pour charger un fichier depuis resources/.
     */
    private static Path loadResource(String name) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("⚠️ Fichier introuvable dans resources : " + name);
        }
        try {
            return Paths.get(url.toURI());
        } catch (Exception ex) {
            throw new IllegalStateException("Erreur lors du chargement du fichier : " + name, ex);
        }
    }
}
