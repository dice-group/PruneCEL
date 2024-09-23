package org.dice_research.cel.refine.suggest.sparql;

import java.util.ArrayList;
import java.util.List;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.SimpleQuantifiedRole;
import org.dice_research.cel.refine.suggest.Suggestor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SparqlBasedSuggestorPrepareTest {

    @Parameters
    public static List<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<>();

        ClassExpression input;
        ClassExpression expectedSugPart;

//        // ⌖
//        input = Suggestor.CONTEXT_POSITION_MARKER;
//        testCases.add(new Object[] { input, null, input });
//
//        // ⌖⊓A
//        input = new Junction(true, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("A"));
//        testCases.add(new Object[] { input, null, input });
//
//        // ⌖⊓¬A
//        input = new Junction(true, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("A", true));
//        testCases.add(new Object[] { input, null, input });
//
//        // ⌖⊔A
//        input = new Junction(false, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("A"));
//        expectedSugPart = new Junction(true, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("A", true));
//        testCases.add(new Object[] { input, new NamedClass("A"), expectedSugPart });
//
//        // ⌖⊔¬A
//        input = new Junction(false, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("A", true));
//        expectedSugPart = new Junction(true, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("A"));
//        testCases.add(new Object[] { input, new NamedClass("A", true), expectedSugPart });
//
//        // ∃role1.(⌖⊓∀role2.⊥)
//        input = new SimpleQuantifiedRole(true, "r1", false, new Junction(true, Suggestor.CONTEXT_POSITION_MARKER,
//                new SimpleQuantifiedRole(false, "r2", false, NamedClass.BOTTOM)));
//        testCases.add(new Object[] { input, null, input });
//
//        // ∀role1.(⌖⊓∀role2.⊥)
//        input = new SimpleQuantifiedRole(false, "r1", false, new Junction(true, Suggestor.CONTEXT_POSITION_MARKER,
//                new SimpleQuantifiedRole(false, "r2", false, NamedClass.BOTTOM)));
//        testCases.add(new Object[] { input, null, input });

        // S⊔(∃m.(⌖⊓B)⊓(¬G⊔P)⊓¬M⊓¬D)
        input = new Junction(false, new NamedClass("S"),
                new Junction(true,
                        new SimpleQuantifiedRole(true, "m", false,
                                new Junction(true, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("B"))),
                        new Junction(false, new NamedClass("G", true), new NamedClass("P")), new NamedClass("M", true),
                        new NamedClass("D", true)));
        expectedSugPart = new Junction(false,
                new Junction(true,
                        new SimpleQuantifiedRole(true, "m", false,
                                new Junction(true, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("B"))),
                        new NamedClass("G", true), new NamedClass("M", true), new NamedClass("D", true),
                        new NamedClass("S", true)),
                new Junction(true,
                        new SimpleQuantifiedRole(true, "m", false,
                                new Junction(true, Suggestor.CONTEXT_POSITION_MARKER, new NamedClass("B"))),
                        new NamedClass("P"), new NamedClass("M", true), new NamedClass("D", true),
                        new NamedClass("S", true)));
        testCases.add(new Object[] { input, new NamedClass("S"), expectedSugPart });

        return testCases;
    }

    protected ClassExpression input;
    protected ClassExpression expectedBasePart;
    protected ClassExpression expectedSugPart;

    public SparqlBasedSuggestorPrepareTest(ClassExpression input, ClassExpression expectedBasePart,
            ClassExpression expectedSugPart) {
        super();
        this.input = input;
        this.expectedBasePart = expectedBasePart;
        this.expectedSugPart = expectedSugPart;
    }

    @Test
    public void test() throws Exception {
        try (SparqlBasedSuggestor suggestor = new SparqlBasedSuggestor(null, null);) {
            SuggestionData data = suggestor.prepareForSuggestion(input, 0, 0);
            Assert.assertEquals(expectedSugPart, data.suggestionPart);
            Assert.assertEquals(expectedBasePart, data.basePart);
        }
    }

}
