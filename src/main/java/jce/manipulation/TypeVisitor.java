package jce.manipulation;

import java.awt.Window.Type;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

/**
 * {@link ASTVisitor} class for {@link Type}s to the manipulate inheritance relations.
 * @author Timur Saglam
 */
public class TypeVisitor extends ASTVisitor {
    private final IJavaProject currentProject;

    /**
     * Basic constructor.
     * @param currentProject is the current {@link IJavaProject}.
     */
    public TypeVisitor(IJavaProject currentProject) {
        this.currentProject = currentProject;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) { // is class
            // TODO (HIGH) change inheritance if it has a ecore equivalent.
            System.err.print(node.getName().getFullyQualifiedName());
            System.err.print(" is a ");
            System.err.println(node.getSuperclassType());

            // step 1: Create a search pattern
            // search methods having "abcde" as name
            String name = node.getName().getFullyQualifiedName();
            System.err.println("start search process for " + name);
            SearchPattern pattern = SearchPattern.createPattern(name, IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS,
                    SearchPattern.R_PREFIX_MATCH);
            // step 2: Create search scope
            // IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
            IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { currentProject }, false);
            // step3: define a result collector
            SearchRequestor requestor = new SearchRequestor() {
                public void acceptSearchMatch(SearchMatch match) {
                    System.err.println("FOUND: " + match.getElement());
                }
            };
            // step4: start searching
            SearchEngine searchEngine = new SearchEngine();
            try {
                searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope, requestor, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }

        }
        return super.visit(node);
    }
}