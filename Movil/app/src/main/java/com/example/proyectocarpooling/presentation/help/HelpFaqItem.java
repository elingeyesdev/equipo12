package com.example.proyectocarpooling.presentation.help;

public final class HelpFaqItem {

    public static final int TYPE_SECTION = 0;
    public static final int TYPE_QUESTION = 1;

    private final int type;
    private final String sectionTitle;
    private final String question;
    private final String answer;
    private boolean expanded;

    public static HelpFaqItem section(String title) {
        return new HelpFaqItem(TYPE_SECTION, title, null, null, false);
    }

    public static HelpFaqItem question(String question, String answer) {
        return new HelpFaqItem(TYPE_QUESTION, null, question, answer, false);
    }

    private HelpFaqItem(int type, String sectionTitle, String question, String answer, boolean expanded) {
        this.type = type;
        this.sectionTitle = sectionTitle;
        this.question = question;
        this.answer = answer;
        this.expanded = expanded;
    }

    public int getType() {
        return type;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
