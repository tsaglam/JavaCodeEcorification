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

    public String parentOf(String path) {
        return path.substring(0, path.lastIndexOf(separator));
    }

    public String nthParentOf(String path, int n) {
        String result = path;
        for (int i = 0; i < n; i++) {
            result = parentOf(result);
        }
        return result;
    }

    public static void main(String[] args) {
        PathHelper p = new PathHelper('/');
        System.err.println(p.append());
        System.err.println(p.append("one"));
        System.err.println(p.append("one", "two"));
        System.err.println(p.append("one", "two", "three"));
        System.err.println(p.append(""));
        System.err.println(p.append("", "two", "three"));
        System.err.println(p.append("one", "", "three"));
        System.err.println(p.append("one", "two", ""));
    }

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
            if (result.startsWith(Character.toString(separator))) {
                return result.substring(1);
            }
            return result;
        }
    }
}