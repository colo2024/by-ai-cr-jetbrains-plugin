package org.example.ai_cr_plugin;

public enum ActionType {
    EXPLAIN("Explain Code", "Explain File", "explain", " /explain"),
    IMPROVE("Improve Code", "Improve File", "improve", " /improve"),
    DOCSTRING("Write Docstring For Code", "Write Docstring For File", "docstring", " /docstring"),
    TEST("Generate UnitTests For Code", "Generate UnitTests For File", "test", " /unit-test");

    private final String editorMenuText;
    private final String projectMenuText;
    private final String actionCode;
    private final String inlayText;

    ActionType(String editorMenuText, String projectMenuText, String actionCode, String inlayText) {
        this.editorMenuText = editorMenuText;
        this.projectMenuText = projectMenuText;
        this.actionCode = actionCode;
        this.inlayText = inlayText;
    }

    public String getEditorMenuText() {
        return editorMenuText;
    }

    public String getProjectMenuText() {
        return projectMenuText;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getInlayText() {
        return inlayText;
    }

    public static ActionType fromProjectMenuText(String menuText) {
        for (ActionType type : values()) {
            if (type.projectMenuText.equals(menuText)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown menu text: " + menuText);
    }

    public static ActionType fromEditorMenuText(String menuText) {
        for (ActionType type : values()) {
            if (type.editorMenuText.equals(menuText)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown editor menu text: " + menuText);
    }
}
