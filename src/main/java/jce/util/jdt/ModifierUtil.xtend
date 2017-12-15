package jce.util.jdt

import org.eclipse.jdt.core.dom.IExtendedModifier
import jce.util.RawTypeUtil
import org.eclipse.jdt.core.dom.Modifier
import org.eclipse.jdt.core.dom.BodyDeclaration

/**
 * Utility class for JDT Modifier functionality.
 */
final class ModifierUtil {

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability")
	}

	/**
	 * Removes private and protected keyword in form of a {@link Modifier} from a type declaration node. Returns the
	 * removed {@link Modifier} or null of none was removed.
	 * @param node is the type declaration node.
	 * @return returns the removed modifier or null if none was removed.
	 */
	def static Modifier removeModifiers(BodyDeclaration node) {
		for (extendedModifier : RawTypeUtil.castList(IExtendedModifier, node.modifiers())) {
			if(extendedModifier.isModifier) { // if is modifier (not annotation)
				val Modifier modifier = extendedModifier as Modifier // cast to modifier
				if(modifier.isPrivate || modifier.isProtected) {
					modifier.delete // remove modifier from node
					return modifier
				}
			}
		}
	}
}
