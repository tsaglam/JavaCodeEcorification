package jce.util;

/**
 * Class that helps to work with paths (file paths, package paths).
 * @author Timur Saglam
 */
public class PathHelper {
    private final char separator;
    private final String separatorString;

    /**
     * Basic constructor.
     * @param separator sets the path separator.
     */
    public PathHelper(char separator) {
        this.separator = separator;
        separatorString = Character.toString(separator);
    }

    /**
     * Appends any number of paths with the separator.
     * @param paths is the array of input paths.
     * @return the new appended path.
     */
    public String append(String... paths) {
        if (paths == null || paths.length == 0) { // if no parameters
            return ""; // return empty string
        } else if (paths.length == 1) { // if one parameter
            return paths[0]; // return it
        } else { // if multiple
            String result = paths[0]; // concatenate all:
            for (int i = 1; i < paths.length; i++) {
                if (paths[i].isEmpty()) {
                    continue;
                }
                result = result + separator + paths[i];
            }
            if (startsWithSeparator(result) && !startsWithSeparator(paths[0])) {
                return result.substring(1);
            }
            return result;
        }
    }

    /**
     * Cuts the first segment of a path.
     * @param path is the path.
     * @return the path without the first segment, or the original if it has no parents.
     */
    public String cutFirstSegment(String path) {
        if (path.contains(separatorString)) {
            return path.substring(path.indexOf(separator) + 1);
        }
        return path;
    }

    /**
     * Cuts the last segment of a path. If the path ends on a separator, it gets removed first.
     * @param path is the original path.
     * @return the path without the last segment.
     */
    public String cutLastSegment(String path) {
        String newPath = removeTrailingSeparator(path);
        if (newPath.contains(separatorString)) {
            return newPath.substring(0, newPath.lastIndexOf(separator));
        }
        return newPath;
    }

    /**
     * Returns the original path without the last n segments. If the path ends on a separator, it gets removed first.
     * @param path is the original path.
     * @param segments specifies the parent index.
     * @return the path without the last n segments.
     */
    public String cutLastSegments(String path, int segments) {
        String result = path;
        for (int i = 0; i < segments; i++) {
            result = cutLastSegment(result);
        }
        return result;
    }

    /**
     * Returns the first segment of the path.
     * @param path is the original path.
     * @return the first segment, or the original path if it has only one segment.
     */
    public String getFirstSegment(String path) {
        if (hasMultipleSegments(path)) {
            return path.substring(0, path.indexOf(separator));
        }
        return path;
    }

    /**
     * Returns the last segment of the original path. If the path ends on a separator, it gets removed first.
     * @param path is the original path.
     * @return the name.
     */
    public String getLastSegment(String path) {
        String newPath = removeTrailingSeparator(path);
        return newPath.substring(newPath.lastIndexOf(separator) + 1);
    }

    /**
     * Checks whether a path has at least two segments.
     * @param path is the path.
     * @return true if it multiple.
     */
    public boolean hasMultipleSegments(String path) {
        return path.contains(separatorString);
    }

    /**
     * Checks if a path starts with the separator character.
     * @param path is the path.
     * @return true if it does.
     */
    public boolean startsWithSeparator(String path) {
        return path.startsWith(separatorString);
    }

    /**
     * Cuts trailing separator if there is one.
     */
    private String removeTrailingSeparator(String path) {
        if (path.endsWith(separatorString)) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Capitalizes the first letter of a {@link String}.
     * @param input is the {@link String} to capitalize.
     * @return the capitalized {@link String}.
     */
    public static String capitalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string cannot be null!");
        } else if (input.length() < 2) {
            return input.toUpperCase();
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}