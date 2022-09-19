package org.dice_group.lpbenchgen.dl;

import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class ManchesterRenderer {

    private static ManchesterOWLSyntaxOWLObjectRendererImpl init() {
        ManchesterOWLSyntaxOWLObjectRendererImpl inst = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        inst.setShortFormProvider(new DefaultPrefixManager());
        return inst;
    }

    private static final ManchesterOWLSyntaxOWLObjectRendererImpl renderer = init();

    public static String render(OWLClassExpression cle) {
        String manchester = renderer.render(cle).replace("\n", "");
        while (manchester.contains("  ")) {
            manchester = manchester.replace("  ", " ");
        }
        return manchester;
    }
}

