package jce.util;

/**
 * Class that helps to work with paths (file paths, package paths).
 * @author Timur Saglam
 */
public class PathHelper {
    private final char separator;

    /**
     * Basic constructor.
     * @param separator sets the path separator.
     */
    public PathHelper(char separator) {
        this.separator = separator;
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
        if (path.contains(Character.toString(separator))) {
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
        if (path.endsWith(Character.toString(separator))) {
            return path.substring(0, path.lastIndexOf(separator, path.length() - 2));
        }
        return path.substring(0, path.lastIndexOf(separator));
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
     * Returns the last segment of the original path.
     * @param path is the original path.
     * @return the name.
     */
    public String getLastSegment(String path) {
        return path.substring(path.lastIndexOf(separator) + 1);
    }

    /**
     * Checks whether a path has at least two segments.
     * @param path is the path.
     * @return true if it multiple.
     */
    public boolean hasMultipleSegments(String path) {
        return path.contains(Character.toString(separator));
    }

    /**
     * Checks if a path starts with the separator character.
     * @param path is the path.
     * @return true if it does.
     */
    public boolean startsWithSeparator(String path) {
        return path.startsWith(Character.toString(separator));
    }
}