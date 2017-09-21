package jce.util.jdt

import org.eclipse.jdt.core.dom.ArrayType
import org.eclipse.jdt.core.dom.ParameterizedType
import org.eclipse.jdt.core.dom.PrimitiveType
import org.eclipse.jdt.core.dom.SimpleType
import org.eclipse.jdt.core.dom.Type

/**
 * Utility class for dealing with {@link Type} objects. Instances of subclasses of Type is
 * often only declared as Type, which is why their functionality is hidden. This class helps to deal with that.
 */
final class TypeUtil {

	private new() {
		throw new AssertionError("Suppress default constructor for noninstantiability")
	}

	/**
	 * Returns the name of a specific {@link Type}.
	 * @param type is the specific {@link Type}.
	 * @return the name of the specific {@link Type}.
	 * @throws an {@link IllegalArgumentException} if it is not possible to get a name from the specific {@link Type}.
	 */
	def static dispatch String getTypeName(Type type) {
		throw new IllegalArgumentException('''Cannot get name from «type.class.name» «type»''')
	}

	def static dispatch String getTypeName(SimpleType type) {
		return type.name.fullyQualifiedName // get fully qualified name
	}

	def static dispatch String getTypeName(PrimitiveType type) {
		return type.primitiveTypeCode.toString // name of primitive type
	}

	def static dispatch String getTypeName(ParameterizedType type) {
		return getTypeName(type.type) // get name of the type of parameterized type
	}

	def static dispatch String getTypeName(ArrayType type) {
		return getTypeName(type.getElementType) // get name of the element type of the array type
	}

	/**
	 * Returns the {@link SimpleType} of a specific {@link Type}. If the {@link Type} is a {@link SimpleType} it is
	 * simply casted. If it is a {@link Type} with a reference to a {@link SimpleType}, its type gets resolved
	 * recursively with this method.
	 * @param type is the specific {@link Type}.
	 * @return the {@link SimpleType}.
	 * @throws an {@link IllegalArgumentException} if it is not possible to get an {@link SimpleType} from the specific {@link Type}.
	 */
	def static dispatch SimpleType getSimpleType(Type type) {
		throw new IllegalArgumentException('''Cannot get simple type from «type.class.name» «type»''')
	}

	def static dispatch SimpleType getSimpleType(SimpleType type) {
		return type // already is simple type
	}

	def static dispatch SimpleType getSimpleType(ParameterizedType type) {
		return getSimpleType(type.type) // get simple type from parameterized types.
	}

	def static dispatch SimpleType getSimpleType(ArrayType type) {
		return getSimpleType(type.getElementType) // get simple type from the element type of the array type
	}
}
