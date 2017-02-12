package jce.codegen;

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
     * Returns the name of the original path. That is the last segment of the original path.
     * @param path is the original path.
     * @return the name.
     */
    public String nameOf(String path) {
        return path.substring(path.lastIndexOf(separator) + 1);
    }

    /**
     * Returns the n-th parent of the original path. That is the original path without the last n - 1 segments.
     * @param path is the original path.
     * @param n specifies the parent index.
     * @return the parent path.
     */
    public String nthParentOf(String path, int n) {
        String result = path;
        for (int i = 0; i < n; i++) {
            result = parentOf(result);
        }
        return result;
    }

    /**
     * Returns the parent of the original path. That is the original path without the last segment.
     * @param path is the original path.
     * @return the parent.
     */
    public String parentOf(String path) {
        if (path.endsWith(Character.toString(separator))) {
            path.substring(0, path.lastIndexOf(separator, path.length() - 2));
        }
        return path.substring(0, path.lastIndexOf(separator));
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