package ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.llm.LlmClient;
import ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.llm.LlmCR;
import ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.llm.LlmCT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("chatBean")
@ViewScoped
public class Bb implements Serializable {

    private String systemRoleText;
    private boolean systemRoleLocked = false;

    private List<SelectItem> systemRoleChoices;

    private String userMessage;
    private String llmResponse;

    private final StringBuilder dialogHistory = new StringBuilder();

    @Inject
    private FacesContext context;

    @Inject
    private LlmCT tavilyClient;

    @Inject
    private LlmCR routingClient;

    @Inject
    private LlmClient baseClient;

    public Bb() {
    }

    public String getSystemRoleText() { return systemRoleText; }
    public void setSystemRoleText(String systemRoleText) { this.systemRoleText = systemRoleText; }

    public boolean isSystemRoleLocked() { return systemRoleLocked; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getLlmResponse() { return llmResponse; }

    public String getDialogHistory() { return dialogHistory.toString(); }

    public void setDialogHistory(String history) {
        dialogHistory.setLength(0);
        dialogHistory.append(history);
    }

    public String send() {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Aucune question saisie", "Merci d’écrire une question."));
            return null;
        }

        try {
            if (dialogHistory.length() == 0 && systemRoleText != null && !systemRoleText.isBlank()) {
                baseClient.applySystemRole(systemRoleText); // ✅ correction ici
                systemRoleLocked = true;
            }

            llmResponse = tavilyClient.ask(userMessage);

            appendToHistory(userMessage, llmResponse);

        } catch (Exception ex) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur lors de l'appel au modèle", ex.getMessage()));
        }

        return null;
    }

    public String resetChat() {
        return "index";
    }

    private void appendToHistory(String user, String reply) {
        dialogHistory.append("Utilisateur:\n")
                .append(user)
                .append("\nRéponse Gemini:\n")
                .append(reply)
                .append("\n\n");
    }

    public List<SelectItem> getSystemRoleChoices() {
        if (systemRoleChoices == null) {
            systemRoleChoices = new ArrayList<>();

            String assistantBase = """
                    You are a friendly assistant. Provide clear and concise answers.
                    If the user asks a question, respond constructively.
                    """;

            systemRoleChoices.add(new SelectItem(assistantBase, "Assistant"));
        }

        return systemRoleChoices;
    }
}
