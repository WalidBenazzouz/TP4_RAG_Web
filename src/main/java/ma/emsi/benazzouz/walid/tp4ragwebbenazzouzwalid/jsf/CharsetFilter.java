package ma.emsi.benazzouz.walid.tp4ragwebbenazzouzwalid.jsf;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Ce filtre assure que toutes les requêtes et réponses utilisent l'encodage UTF-8.
 * Il garantit l'affichage correct des caractères accentués dans l'application JSF.
 */
@WebFilter("/*")
public class CharsetFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // on utilise UTF-8 pour l'entrée
        req.setCharacterEncoding("UTF-8");

        // et on force l'utilisation de UTF-8 pour la sortie
        if (res instanceof HttpServletResponse httpRes) {
            httpRes.setCharacterEncoding("UTF-8");
            httpRes.setContentType("text/html; charset=UTF-8");
        }

        // continuer le traitement
        chain.doFilter(req, res);
    }
}