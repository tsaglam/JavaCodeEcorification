package jce.codemanipulation.origin;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import eme.generator.GeneratedEcoreMetamodel;
import jce.properties.EcorificationProperties;
import jce.util.EcoreUtil;
import jce.util.PathHelper;
import jce.util.logging.MonitorFactory;

/**
 * {@link ASTVisitor} that removes all private non-static fields and their access methods from a compilation unit that
 * have a counterpart in the Ecore metamodel which was extracted from the origin code.
 * @author Timur Saglam
 */
public class MemberRemovalVisitor extends ASTVisitor {
    private final GeneratedEcoreMetamodel metamodel;
    private final IProgressMonitor monitor;
    private final List<String> removedFields;

    /**
     * Basic constructor.
     * @param metamodel is the Ecore metamodel which was extracted from the origin code.
     * @param properties are the {@link EcorificationProperties}.
     */
    public MemberRemovalVisitor(GeneratedEcoreMetamodel metamodel, EcorificationProperties properties) {
        this.metamodel = metamodel;
        monitor = MonitorFactory.createProgressMonitor(LogManager.getLogger(getClass().getName()), properties);
        removedFields = new LinkedList<>();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface() && node.isPackageMemberTypeDeclaration()) { // if is class, manipulate inheritance:
            removeFields(node);
            removeAccessMethods(node);
            monitor.beginTask("Removed fields with their access methods from " + node.getName().getIdentifier() + ": " + removedFields, 0);
        }
        return super.visit(node);
    }

    /**
     * Checks whether a {@link MethodDeclaration} is an access method.
     */
    private boolean isAccessMethod(MethodDeclaration method) {
        String[] prefixes = { "get", "set", "is" }; // access method prefixes
        for (String prefix : prefixes) {
            for (String field : removedFields) {
                if (method.getName().getIdentifier().equals(prefix + PathHelper.capitalize(field))) {
                    return true; // is access method if method name matches prefix + field name once
                }
            }
        }
        return false;
    }

    /**
     * Checks whether a variable declaration fragment was generated as part of the Ecore model code by searching the
     * correlating {@link EStructuralFeature} in the extracted Ecore metamodel.
     */
    private boolean isGenerated(VariableDeclarationFragment fragment, TypeDeclaration type) {
        String typeName = type.getName().resolveTypeBinding().getQualifiedName(); // fully qualified name of class
        String fieldName = fragment.getName().getIdentifier(); // name of field
        return EcoreUtil.findEStructuralFeature(fieldName, typeName, metamodel.getRoot()) != null;
    }

    /**
     * Checks whether a {@link FieldDeclaration} is unnecessary. That means it should be removed by this visitor.
     */
    private boolean isUnnecessary(FieldDeclaration field) {
        return !Modifier.isStatic(field.getModifiers()) && Modifier.isPrivate(field.getModifiers());
    }

    /**
     * Removes any {@link MethodDeclaration} of a {@link TypeDeclaration} which is an access method for a previously
     * removed {@link FieldDeclaration}.
     */
    private void removeAccessMethods(TypeDeclaration type) {
        for (MethodDeclaration method : type.getMethods()) {
            if (isAccessMethod(method)) {
                method.delete();
            }
        }
    }

    /**
     * Removes any non-static {@link FieldDeclaration} of a {@link TypeDeclaration}. Saves the identifiers of the
     * removed fields in the {@link MemberRemovalVisitor#removedFields} list.
     */
    private void removeFields(TypeDeclaration type) {
        for (FieldDeclaration field : type.getFields()) { // for every field
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) field.fragments().get(0);
            if (isUnnecessary(field) && isGenerated(fragment, type)) { // if not static
                field.delete(); // delete
                removedFields.add(fragment.getName().getIdentifier());
            }
        }
    }
}