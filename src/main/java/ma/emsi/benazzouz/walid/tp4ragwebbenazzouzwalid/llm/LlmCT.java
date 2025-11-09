package ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.llm;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service permettant d'interroger soit des documents locaux,
 * soit le web (via Tavily), selon la nature de la question.
 */
@ApplicationScoped
public class LlmCT{

    private final Assistant chatAgent;

    public LlmCT() {

        String geminiKey = System.getenv("GEMINI_API_KEY");
        String tavilyKey = System.getenv("TAVILY_API_KEY");

        if (geminiKey == null || geminiKey.isBlank()) {
            throw new IllegalStateException("⚠️ GEMINI_API_KEY est manquant.");
        }
        if (tavilyKey == null || tavilyKey.isBlank()) {
            throw new IllegalStateException("⚠️ TAVILY_API_KEY est manquant.");
        }

        // Modèle conversationnel
        ChatModel llm = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-1.5-flash")
                .temperature(0.3)
                .build();

        // Parser PDF
        var parser = new ApacheTikaDocumentParser();

        // Chargement des documents
        Document docIA = loadPdf("rag-2.pdf", parser);
        Document docSport = loadPdf("sport.pdf", parser);

        // Split en segments exploitables
        var splitter = DocumentSplitters.recursive(300, 30);
        List<TextSegment> segIA = splitter.split(docIA);
        List<TextSegment> segSport = splitter.split(docSport);

        // Embeddings + boutique en mémoire
        EmbeddingModel embedder = new AllMiniLmL6V2EmbeddingModel();
        EmbeddingStore<TextSegment> vectorStore = new InMemoryEmbeddingStore<>();

        vectorStore.addAll(embedder.embedAll(segIA).content(), segIA);
        vectorStore.addAll(embedder.embedAll(segSport).content(), segSport);

        // Récupération locale
        ContentRetriever localRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(vectorStore)
                .embeddingModel(embedder)
                .maxResults(3)
                .minScore(0.48)
                .build();

        // Recherche Web Tavily
        var tavilySearch = TavilyWebSearchEngine.builder()
                .apiKey(tavilyKey)
                .build();

        ContentRetriever webRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(tavilySearch)
                .maxResults(3)
                .build();

        // Routage automatique
        var router = new DefaultQueryRouter(List.of(localRetriever, webRetriever));

        // RAG
        RetrievalAugmentor ragLayer = DefaultRetrievalAugmentor.builder()
                .queryRouter(router)
                .build();

        // Assistant final
        this.chatAgent = AiServices.builder(Assistant.class)
                .chatModel(llm)
                .retrievalAugmentor(ragLayer)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * Appelle le modèle avec augmentation contextuelle.
     */
    public String ask(String prompt) {
        return chatAgent.chat(prompt);
    }

    /**
     * Charge un PDF du dossier resources/.
     */
    private static Document loadPdf(String name, ApacheTikaDocumentParser parser) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
        if (resource == null) {
            throw new IllegalStateException("📄 Fichier introuvable dans resources : " + name);
        }
        try {
            Path path = Paths.get(resource.toURI());
            return FileSystemDocumentLoader.loadDocument(path, parser);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger le document : " + name, e);
        }
    }
}
