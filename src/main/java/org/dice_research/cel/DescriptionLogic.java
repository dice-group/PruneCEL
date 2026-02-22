package org.dice_research.cel;

import java.util.BitSet;

public class DescriptionLogic implements Cloneable {
    /**
     * The name of the description logic.
     */
    protected String name;

    /**
     * 
     */
    protected boolean supportsAtomicNegation = false;

    /**
     * 
     */
    protected boolean supportsConceptIntersection = false;

    /**
     * 
     */
    protected boolean supportsUniversalRestrictions = false;

    /**
     * Supports E = full existential qualification.
     */
    protected boolean supportsExistentialQuantification = false;

    /**
     * Supports U = Concept union.
     */
    protected boolean supportsConceptUnion = false;

    /**
     * Supports C = Complex concept negation.
     */
    protected boolean supportsComplexConceptNegation = false;

    /**
     * Supports H = Role hierarchy.
     */
    protected boolean supportsRoleHierarchy = false;

    /**
     * Supports O = Nominals.
     */
    protected boolean supportsNominals = false;

    /**
     * Supports I = Inverse properties.
     */
    protected boolean supportsInverseProperties = false;

    /**
     * Supports N = Cardinality restrictions.
     */
    protected boolean supportsCardinalityRestrictions = false;

    /**
     * Supports Q = Qualified cardinality (cardinality restrictions that have
     * fillers other than ⊤).
     */
    protected boolean supportsQualifiedCardinality = false;

    /**
     * Supports (D) = Use of datatype properties, data values or data types.
     */
    protected boolean supportsDataValues = false;

    protected DescriptionLogic() {
    }

    protected DescriptionLogic(String name, boolean supportsAtomicNegation, boolean supportsConceptIntersection,
            boolean supportsUniversalRestrictions, boolean supportsExistentialQuantification,
            boolean supportsConceptUnion, boolean supportsComplexConceptNegation, boolean supportsRoleHierarchy,
            boolean supportsNominals, boolean supportsInverseProperties, boolean supportsCardinalityRestrictions,
            boolean supportsQualifiedCardinality, boolean supportsDataValues) {
        super();
        this.name = name;
        this.supportsAtomicNegation = supportsAtomicNegation;
        this.supportsConceptIntersection = supportsConceptIntersection;
        this.supportsUniversalRestrictions = supportsUniversalRestrictions;
        this.supportsExistentialQuantification = supportsExistentialQuantification;
        this.supportsConceptUnion = supportsConceptUnion;
        this.supportsComplexConceptNegation = supportsComplexConceptNegation;
        this.supportsRoleHierarchy = supportsRoleHierarchy;
        this.supportsNominals = supportsNominals;
        this.supportsInverseProperties = supportsInverseProperties;
        this.supportsCardinalityRestrictions = supportsCardinalityRestrictions;
        this.supportsQualifiedCardinality = supportsQualifiedCardinality;
        this.supportsDataValues = supportsDataValues;
    }

    public boolean supportsAtomicNegation() {
        return supportsAtomicNegation;
    }

    public boolean supportsConceptIntersection() {
        return supportsConceptIntersection;
    }

    public boolean supportsUniversalRestrictions() {
        return supportsUniversalRestrictions;
    }

    /**
     * Supports E = full existential qualification.
     */
    public boolean supportsExistentialQuantification() {
        return supportsExistentialQuantification;
    }

    /**
     * Supports U = Concept union.
     */
    public boolean supportsConceptUnion() {
        return supportsConceptUnion;
    }

    /**
     * Supports C = Complex concept negation.
     */
    public boolean supportsComplexConceptNegation() {
        return supportsComplexConceptNegation;
    }

    /**
     * Supports H = Role hierarchy.
     */
    public boolean supportsRoleHierarchy() {
        return supportsRoleHierarchy;
    }

    /**
     * Supports O = Nominals.
     */
    public boolean supportsNominals() {
        return supportsNominals;
    }

    /**
     * Supports I = Inverse properties.
     */
    public boolean supportsInverseProperties() {
        return supportsInverseProperties;
    }

    /**
     * Supports I = Cardinality restrictions.
     */
    public boolean supportsCardinalityRestrictions() {
        return supportsCardinalityRestrictions;
    }

    /**
     * Supports I = Qualified cardinality (cardinality restrictions that have
     * fillers other than ⊤).
     */
    public boolean supportsQualifiedCardinality() {
        return supportsQualifiedCardinality;
    }

    /**
     * Supports (D) = Use of datatype properties, data values or data types.
     */
    public boolean supportsDataValues() {
        return supportsDataValues;
    }

    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * @param supportsAtomicNegation the supportsAtomicNegation to set
     */
    protected void setSupportsAtomicNegation(boolean supportsAtomicNegation) {
        this.supportsAtomicNegation = supportsAtomicNegation;
    }

    /**
     * @param supportsConceptIntersection the supportsConceptIntersection to set
     */
    protected void setSupportsConceptIntersection(boolean supportsConceptIntersection) {
        this.supportsConceptIntersection = supportsConceptIntersection;
    }

    /**
     * @param supportsUniversalRestrictions the supportsUniversalRestrictions to set
     */
    protected void setSupportsUniversalRestrictions(boolean supportsUniversalRestrictions) {
        this.supportsUniversalRestrictions = supportsUniversalRestrictions;
    }

    /**
     * @param supportsExistentialQuantification the
     *                                          supportsExistentialQuantification to
     *                                          set
     */
    protected void setSupportsExistentialQuantification(boolean supportsExistentialQuantification) {
        this.supportsExistentialQuantification = supportsExistentialQuantification;
    }

    /**
     * @param supportsConceptUnion the supportsConceptUnion to set
     */
    protected void setSupportsConceptUnion(boolean supportsConceptUnion) {
        this.supportsConceptUnion = supportsConceptUnion;
    }

    /**
     * @param supportsComplexConceptNegation the supportsComplexConceptNegation to
     *                                       set
     */
    protected void setSupportsComplexConceptNegation(boolean supportsComplexConceptNegation) {
        this.supportsComplexConceptNegation = supportsComplexConceptNegation;
    }

    /**
     * @param supportsRoleHierarchy the supportsRoleHierarchy to set
     */
    protected void setSupportsRoleHierarchy(boolean supportsRoleHierarchy) {
        this.supportsRoleHierarchy = supportsRoleHierarchy;
    }

    /**
     * @param supportsNominals the supportsNominals to set
     */
    protected void setSupportsNominals(boolean supportsNominals) {
        this.supportsNominals = supportsNominals;
    }

    /**
     * @param supportsInverseProperties the supportsInverseProperties to set
     */
    protected void setSupportsInverseProperties(boolean supportsInverseProperties) {
        this.supportsInverseProperties = supportsInverseProperties;
    }

    /**
     * @param supportsCardinalityRestrictions the supportsCardinalRestrictions to
     *                                        set
     */
    protected void setSupportsCardinalityRestrictions(boolean supportsCardinalityRestrictions) {
        this.supportsCardinalityRestrictions = supportsCardinalityRestrictions;
    }

    /**
     * @param supportsQualifiedCardinality the supportsQualifiedCardinality to set
     */
    protected void setSupportsQualifiedCardinality(boolean supportsQualifiedCardinality) {
        this.supportsQualifiedCardinality = supportsQualifiedCardinality;
    }

    /**
     * @param supportsDataValues the supportsDataValues to set
     */
    protected void setSupportsDataValues(boolean supportsDataValues) {
        this.supportsDataValues = supportsDataValues;
    }

    @Override
    public Object clone() {
        return new DescriptionLogic(name, supportsAtomicNegation, supportsConceptIntersection,
                supportsUniversalRestrictions, supportsExistentialQuantification, supportsConceptUnion,
                supportsComplexConceptNegation, supportsRoleHierarchy, supportsNominals, supportsInverseProperties,
                supportsCardinalityRestrictions, supportsQualifiedCardinality, supportsDataValues);
    }

    public static DescriptionLogic parse(String name) {
        DescriptionLogic logic = new DescriptionLogic();
        String baseLang = "";
        if (name.contains("EL")) {
            baseLang = "EL";
            logic.setSupportsConceptIntersection(true);
            logic.setSupportsExistentialQuantification(true);
        } else if (name.contains("AL")) {
            baseLang = "AL";
            logic.setSupportsAtomicNegation(true);
            logic.setSupportsConceptIntersection(true);
            logic.setSupportsUniversalRestrictions(true);
            logic.setSupportsConceptUnion(true);
            logic.setSupportsExistentialQuantification(true);
        }
        char letters[] = name.toCharArray();
        BitSet nameParts = new BitSet(); // CSRHOIQND
        for (int i = 0; i < letters.length; ++i) {
            switch (letters[i]) {
            case 'a': // falls through
            case 'A':
            case 'e':
            case 'E':
            case 'l':
            case 'L': {
                // nothing to do since base languages are handled above
                break;
            }
            case 's':
            case 'S': {
                logic.setSupportsAtomicNegation(true);
                logic.setSupportsConceptIntersection(true);
                logic.setSupportsUniversalRestrictions(true);
                logic.setSupportsConceptUnion(true);
                logic.setSupportsExistentialQuantification(true);
                // With transitive roles (?)
                nameParts.set(1);
                // falls through, S comes with C
            }
            case 'c':
            case 'C': {
                logic.setSupportsComplexConceptNegation(true);
                nameParts.set(0);
                break;
            }
            // case 'r':
            // case 'R': {
            // logic.setSupportsQualifiedCardinality(true);
            // nameParts.set(2);
            // // falls through, R comes with H
            // }
            case 'h':
            case 'H': {
                logic.setSupportsRoleHierarchy(true);
                nameParts.set(3);
                break;
            }
            case 'o':
            case 'O': {
                logic.setSupportsNominals(true);
                nameParts.set(4);
                break;
            }
            case 'i':
            case 'I': {
                logic.setSupportsInverseProperties(true);
                nameParts.set(5);
                break;
            }
            case 'q':
            case 'Q': {
                logic.setSupportsQualifiedCardinality(true);
                nameParts.set(6);
                // falls through, Q comes with N
            }
            case 'n':
            case 'N': {
                logic.setSupportsCardinalityRestrictions(true);
                nameParts.set(7);
                break;
            }
            case 'd':
            case 'D': {
                logic.setSupportsDataValues(true);
                nameParts.set(8);
                break;
            }
            case '(':
            case ')': {
                // ignore these letters
                break;
            }
            default:
                throw new IllegalArgumentException("Unexpected description logic value: " + letters[i]);
            }
        }
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(baseLang);
        for (int i = 0; i < 9; ++i) {
            if (nameParts.get(i)) {
                switch (i) {
                case 0: {
                    if (!nameParts.get(1)) {
                    nameBuilder.append('C');
                    }
                    break;
                }
                case 1: {
                    nameBuilder.append('S');
                    break;
                }
                case 2: {
                    nameBuilder.append('R');
                    break;
                }
                case 3: {
                    if (!nameParts.get(2)) {
                        nameBuilder.append('H');
                    }
                    break;
                }
                case 4: {
                    nameBuilder.append('O');
                    break;
                }
                case 5: {
                    nameBuilder.append('I');
                    break;
                }
                case 6: {
                    nameBuilder.append('Q');
                    break;
                }
                case 7: {
                    if (!nameParts.get(6)) {
                        nameBuilder.append('N');
                    }
                    break;
                }
                case 8: {
                    nameBuilder.append("(D)");
                    break;
                }
                default:
                    // nothing to do
                }
            }
        }
        logic.setName(nameBuilder.toString());
        return logic;
    }

}