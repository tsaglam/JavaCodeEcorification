package jce.util;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * A utility class for searching elements in Ecore metamodels.
 * @author Timur Saglam
 */
public final class MetamodelSearcher {
    private final static PathHelper PATH = new PathHelper('.');

    private MetamodelSearcher() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Finds a specific {@link EClass} in a {@link EPackage} and its subpackages.
     * @param fullName is the fully qualified name of the desired {@link EClass}.
     * @param ePackage is the {@link EPackage} which contains the {@link EClass}.
     * @return the {@link EClass} or null if there is none with the specified name.
     */
    public static EClass findEClass(String fullName, EPackage ePackage) {
        String eClassName = PATH.getLastSegment(fullName);
        EPackage parent = findEPackage(PATH.cutLastSegment(fullName), ePackage);
        for (EClassifier classifier : parent.getEClassifiers()) {
            if (classifier instanceof EClass && isSame(classifier, eClassName)) {
                return (EClass) classifier;
            }
        }
        return null;
    }

    /**
     * Finds a specific {@link EPackage} which is directly or indirectly a subpackage of a given {@link EPackage}.
     * @param fullName is the fully qualified name of the desired {@link EPackage}.
     * @param ePackage is the {@link EPackage} which contains the subpackage.
     * @return the subpackage or null if there is none with the specified name.
     */
    public static EPackage findEPackage(String fullName, EPackage ePackage) {
        for (EPackage subpackage : ePackage.getESubpackages()) {
            if (isSame(subpackage, PATH.getFirstSegment(fullName))) {
                if (!PATH.hasMultipleSegments(fullName)) {
                    return subpackage;
                } else {
                    return findEPackage(PATH.cutFirstSegment(fullName), subpackage);
                }
            }
        }
        return null;
    }

    /**
     * Finds a specific {@link EStructuralFeature} in an {@link EClass}.
     * @param fullName is the fully qualified name of the desired {@link EStructuralFeature}.
     * @param eClass is the {@link EClass} which contains the {@link EStructuralFeature}.
     * @return the {@link EStructuralFeature} or null if there is none with the specified name.
     */
    public static EStructuralFeature findEStructuralFeature(String fullName, EClass eClass) {
        for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
            if (isSame(feature, fullName)) {
                return feature;
            }
        }
        return null;
    }

    /**
     * Finds a specific {@link EStructuralFeature} in an {@link EPackage}.
     * @param fullName is the fully qualified name of the desired {@link EStructuralFeature}.
     * @param eClassName is the fully qualified name of the {@link EClass} that contains the {@link EStructuralFeature}.
     * @param ePackage ePackage is the {@link EPackage} which contains the {@link EClass} that contains the
     * {@link EStructuralFeature}.
     * @return the {@link EStructuralFeature} or null if there is none with the specified name.
     */
    public static EStructuralFeature findEStructuralFeature(String fullName, String eClassName, EPackage ePackage) {
        EClass eClass = findEClass(eClassName, ePackage);
        if (eClass != null) {
            return findEStructuralFeature(fullName, eClass);
        }
        return null;
    }

    /**
     * Compares the name of an ENamedElement with a String.
     */
    private static boolean isSame(ENamedElement element, String elementName) {
        return element.getName().equals(elementName);
    }
}
