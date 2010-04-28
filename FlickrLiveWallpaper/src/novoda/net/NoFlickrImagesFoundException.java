
package novoda.net;

public class NoFlickrImagesFoundException extends Exception {

    private static final long serialVersionUID = 56826L;

    private int id; 

    private String classname;

    private String method;

    private String message;

    private NoFlickrImagesFoundException previous = null;

    private String separator = "\n";

    public NoFlickrImagesFoundException() {
        this.id = 000000;
        this.classname = "none_set";
        this.method = "none_set";
        this.message = "No Flickr images were found";
        this.previous = null;        
    }
    
    public NoFlickrImagesFoundException(int id, String classname, String method, String message,  NoFlickrImagesFoundException previous) {
        this.id = id;
        this.classname = classname;
        this.method = method;
        this.message = message;
        this.previous = previous;
    }

    public String traceBack() {
        return traceBack("\n");
    }

    public String traceBack(String sep) {
        this.separator = sep;
        int level = 0;
        NoFlickrImagesFoundException e = this;
        String text = line("Calling sequence (top to bottom)");
        while (e != null) {
            level++;
            text += line("--level " + level + "--------------------------------------");
            text += line("Class/Method: " + e.classname + "/" + e.method);
            text += line("Id          : " + e.id);
            text += line("Message     : " + e.message);
            e = e.previous;
        }
        return text;
    }

    private String line(String s) {
        return s + separator;
    }

}
