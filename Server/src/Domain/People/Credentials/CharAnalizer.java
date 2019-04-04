// CRIS

package Domain.People.Credentials;

// CharAnalizer è preposto al riconoscimento dei caratteri (se normali, maiuscoli, numerici o speciali)
// e alla loro stampa
public class CharAnalizer {
    private String line;

    public CharAnalizer(String line) {
        this.line = line;
    }

    protected boolean isLetter() {
        for(char c: line.toCharArray()) {
            if(Character.isLetter(c)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isUpperCase() {
        for(char c: line.toCharArray()) {
            if(Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isDigit() {
        for(char c: line.toCharArray()) {
            if(Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSpecialChar() {
        for(char c: line.toCharArray()) {
            if(!Character.isLetter(c) && !Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    protected String getLine() {
        return line;
    }
}
