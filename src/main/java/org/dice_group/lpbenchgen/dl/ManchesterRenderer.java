package org.dice_group.lpbenchgen.dl;

import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class ManchesterRenderer {
    private static final ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
    private static boolean once = false;

    synchronized private static void init() {
        if (!once) {
            renderer.setShortFormProvider(new DefaultPrefixManager());
            once = true;
        }
    }

    public static String render(OWLClassExpression cle) {

        if (!once) {
            init();
        }
        String manchester = renderer.render(cle).replace("\n", "");
        while (manchester.contains("  ")) {
            manchester = manchester.replace("  ", " ");
        }
        return manchester;
    }

    public static String renderNNF(OWLClassExpression cle) {
        return render(cle.getNNF());
    }
}
