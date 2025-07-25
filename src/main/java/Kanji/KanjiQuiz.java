package Kanji;

public class KanjiQuiz {
	private String kanji;
	private String reading;
	private String meaning;
	
	public KanjiQuiz(String kanji, String reading, String meaning) {
		this.kanji = kanji; this.reading = reading; this.meaning = meaning;
	}
	
	public String getKanji() {return kanji;}
	public String getReading() {return reading;}
	public String getMeaning() {return meaning;}
	


}
