import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Parser {
    private Set<String> options;

    public Parser(Set<String> options) {
        this.options = options;
    }

    public void setOptions(Set<String> options) {
        this.options = options;
    }

    public Map<String, String> parse(final String str) throws ParserException {
        Map<String, String> res = new HashMap<>();

        String[] p = str.split(" ");
        for (int i = 0; i < p.length;) {
            String opt = p[i];
            if (options.contains(opt)) {
                if (res.containsKey(opt)) {
                    throw new ParserException("Ambiguous " + opt + " option");
                }
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < p.length && !options.contains(p[i])) {
                    sb.append(p[i]).append(" ");
                    i++;
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                }
                res.put(opt, sb.toString());
            } else {
                i++;
            }
        }

        return res;
    }
}
