package Kanji;

public class CheckResponse {
    private boolean correct;

    public CheckResponse(boolean correct) {
        this.correct = correct;
    }

    public boolean isCorrect() {return correct;}

    public void setCorrect(boolean correct) {this.correct = correct;}
}
