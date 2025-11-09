// --- Copie du contenu d'un champ texte ---
function copyToClipboard(elementId) {
    const element = document.getElementById(elementId);
    if (!element) {
        alert("Élément introuvable : " + elementId);
        return;
    }

    element.select();
    document.execCommand("copy");
    alert("Contenu copié depuis : " + elementId);
}

// --- Réinitialisation des zones de question/réponse ---
function toutEffacer() {
    const question = document.getElementById("form:question");
    const reponse = document.getElementById("form:reponse");

    if (question) question.value = "";
    if (reponse) reponse.value = "";
}
